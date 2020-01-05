package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.helpers.ext.ecs.request.EcsBGServiceSetupRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceSetupResponse;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class EcsBlueGreenSetupCommandHandler extends EcsCommandTaskHandler {
  private static final String DELIMITER = "__";
  @Inject private AwsHelperService awsHelperService;
  @Inject private EcsSetupCommandTaskHelper ecsSetupCommandTaskHelper;
  @Inject private AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Inject private EcsContainerService ecsContainerService;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    EcsServiceSetupResponse ecsCommandResponse = EcsServiceSetupResponse.builder()
                                                     .isBlueGreen(true)
                                                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                     .build();
    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;

    if (!(ecsCommandRequest instanceof EcsBGServiceSetupRequest)) {
      ecsCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      ecsCommandResponse.setOutput("Invalid Request Type: Expected was : EcsBGServiceSetupRequest");
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .ecsCommandResponse(ecsCommandResponse)
          .build();
    }

    try {
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest = (EcsBGServiceSetupRequest) ecsCommandRequest;

      EcsSetupParams setupParams = ecsBGServiceSetupRequest.getEcsSetupParams();
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
          ContainerSetupCommandUnitExecutionData.builder();
      commandExecutionDataBuilder.ecsRegion(setupParams.getRegion());
      setTargetGroupForProdListener(encryptedDataDetails, ecsBGServiceSetupRequest, setupParams,
          commandExecutionDataBuilder, executionLogCallback);
      createStageListenerNewServiceIfRequired(encryptedDataDetails, ecsBGServiceSetupRequest, executionLogCallback);
      commandExecutionDataBuilder.stageEcsListener(setupParams.getStageListenerArn());
      commandExecutionDataBuilder.targetGroupForNewService(setupParams.getTargetGroupArn());
      commandExecutionDataBuilder.targetGroupForExistingService(setupParams.getTargetGroupArn2());

      TaskDefinition taskDefinition = ecsSetupCommandTaskHelper.createTaskDefinition(
          ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails, ecsBGServiceSetupRequest.getServiceVariables(),
          ecsBGServiceSetupRequest.getSafeDisplayServiceVariables(), executionLogCallback, setupParams);

      createServiceForBlueGreen(setupParams, taskDefinition,
          aSettingAttribute().withValue(ecsBGServiceSetupRequest.getAwsConfig()).build(), encryptedDataDetails,
          commandExecutionDataBuilder, executionLogCallback);

      ecsCommandResponse.setSetupData(commandExecutionDataBuilder.build());
    } catch (Exception ex) {
      logger.error("Completed operation with errors");
      logger.error(ExceptionUtils.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);

      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      ecsCommandResponse.setCommandExecutionStatus(commandExecutionStatus);
      ecsCommandResponse.setOutput(ExceptionUtils.getMessage(ex));
    }
    return EcsCommandExecutionResponse.builder()
        .commandExecutionStatus(commandExecutionStatus)
        .ecsCommandResponse(ecsCommandResponse)
        .build();
  }

  private void createStageListenerNewServiceIfRequired(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, ExecutionLogCallback executionLogCallback) {
    EcsSetupParams setupParams = ecsBGServiceSetupRequest.getEcsSetupParams();
    if (isNotBlank(setupParams.getStageListenerArn())) {
      String targetGroupForNewService = getTargetGroupForListener(encryptedDataDetails, ecsBGServiceSetupRequest,
          setupParams, setupParams.getStageListenerArn(), executionLogCallback);
      setupParams.setTargetGroupArn(targetGroupForNewService);
    } else {
      if (isNotBlank(setupParams.getTargetGroupArn())) {
        // This is just validation. Making Sure Target Group to be used with New Service Exists.
        validateTargetGroupProvidedForNewService(
            encryptedDataDetails, ecsBGServiceSetupRequest, setupParams, executionLogCallback);
      } else {
        cloneListenerProdListenerIfRequired(
            encryptedDataDetails, ecsBGServiceSetupRequest, setupParams, executionLogCallback);
      }
    }
  }

  private void cloneListenerProdListenerIfRequired(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, EcsSetupParams setupParams,
      ExecutionLogCallback executionLogCallback) {
    List<AwsElbListener> listeners =
        awsElbHelperServiceDelegate.getElbListenersForLoadBalaner(ecsBGServiceSetupRequest.getAwsConfig(),
            encryptedDataDetails, setupParams.getRegion(), setupParams.getLoadBalancerName());

    // Check if any listener already exists on port mentioned for stageListener
    Optional<AwsElbListener> optionalListener =
        listeners.stream()
            .filter(listener -> Integer.parseInt(setupParams.getStageListenerPort()) == listener.getPort().intValue())
            .findFirst();

    if (optionalListener.isPresent()) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("Found existing Listener on Port:{ ")
                                                .append(setupParams.getStageListenerPort())
                                                .append("}. ListenerARN: ")
                                                .append(optionalListener.get().getListenerArn())
                                                .append(", Protocol: ")
                                                .append(optionalListener.get().getProtocol())
                                                .toString());

      setupParams.setStageListenerArn(optionalListener.get().getListenerArn());
      // Find out target group this listener is forwarding requests to
      Listener listener = awsElbHelperServiceDelegate.getElbListener(ecsBGServiceSetupRequest.getAwsConfig(),
          encryptedDataDetails, ecsBGServiceSetupRequest.getRegion(), optionalListener.get().getListenerArn());
      String targetGroupArn = ecsSetupCommandTaskHelper.getTargetGroupForDefaultAction(listener, executionLogCallback);
      setupParams.setTargetGroupArn(targetGroupArn);
      return;
    }

    // First create StageTargetGroup if doesn't exist
    cloneTargetGroupFromProdTargetGroupIfRequired(
        encryptedDataDetails, ecsBGServiceSetupRequest, setupParams, executionLogCallback);
    // Now create Listener
    Listener listener = awsElbHelperServiceDelegate.createStageListener(ecsBGServiceSetupRequest.getAwsConfig(),
        encryptedDataDetails, setupParams.getRegion(), setupParams.getProdListenerArn(),
        Integer.parseInt(setupParams.getStageListenerPort()), setupParams.getTargetGroupArn());
    setupParams.setStageListenerArn(listener.getListenerArn());
  }

  private void cloneTargetGroupFromProdTargetGroupIfRequired(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, EcsSetupParams setupParams,
      ExecutionLogCallback executionLogCallback) {
    // Get Prod Target Group
    String prodTargetGroupArn = setupParams.getTargetGroupArn2();
    Optional<TargetGroup> optionalProdTargetGroup = awsElbHelperServiceDelegate.getTargetGroup(
        ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails, setupParams.getRegion(), prodTargetGroupArn);
    TargetGroup prodTargetGroup = optionalProdTargetGroup.get();

    // Check if Stage Target Group already exists
    Optional<TargetGroup> optionalStageTargetGroup =
        awsElbHelperServiceDelegate.getTargetGroupByName(ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails,
            setupParams.getRegion(), format("HarnessClone-%s", prodTargetGroup.getTargetGroupName()));

    if (!optionalStageTargetGroup.isPresent()) {
      executionLogCallback.saveExecutionLog("Creating Target Group To be used with {Green} Service");
      TargetGroup stageTargetGroup = awsElbHelperServiceDelegate.cloneTargetGroup(
          ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails, setupParams.getRegion(), prodTargetGroupArn,
          format("HarnessClone-%s", prodTargetGroup.getTargetGroupName()));
      setupParams.setTargetGroupArn(stageTargetGroup.getTargetGroupArn());
    } else {
      setupParams.setTargetGroupArn(optionalStageTargetGroup.get().getTargetGroupArn());
      executionLogCallback.saveExecutionLog(new StringBuilder(64)
                                                .append("Using Stage Target Group: ")
                                                .append(optionalStageTargetGroup.get().getTargetGroupArn())
                                                .toString());
    }
  }

  private void validateTargetGroupProvidedForNewService(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, EcsSetupParams setupParams,
      ExecutionLogCallback executionLogCallback) {
    Optional<TargetGroup> optionalGroup =
        awsElbHelperServiceDelegate.getTargetGroup(ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails,
            setupParams.getRegion(), setupParams.getTargetGroupArn());
    if (!optionalGroup.isPresent()) {
      String errorMsg = new StringBuilder(128)
                            .append("Invalid TargetGroupArn mentioned: ")
                            .append(setupParams.getTargetGroupArn())
                            .append(", Could not find any target Group")
                            .toString();

      executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  private void setTargetGroupForProdListener(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, EcsSetupParams setupParams,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    // This is targetGroupArn currently associated with ProdListener for ELB
    String prodTargetGroupArn = getTargetGroupForListener(encryptedDataDetails, ecsBGServiceSetupRequest, setupParams,
        setupParams.getProdListenerArn(), executionLogCallback);
    setupParams.setTargetGroupArn2(prodTargetGroupArn);
  }

  private String getTargetGroupForListener(List<EncryptedDataDetail> encryptedDataDetails,
      EcsBGServiceSetupRequest ecsBGServiceSetupRequest, EcsSetupParams setupParams, String listenerArn,
      ExecutionLogCallback executionLogCallback) {
    Listener listener = awsElbHelperServiceDelegate.getElbListener(
        ecsBGServiceSetupRequest.getAwsConfig(), encryptedDataDetails, setupParams.getRegion(), listenerArn);

    Optional<Action> forwardAction =
        listener.getDefaultActions().stream().filter(action -> "forward".equals(action.getType())).findFirst();

    // Listener should have default action set to forward traffic to target group.
    if (!forwardAction.isPresent()) {
      executionLogCallback.saveExecutionLog(
          "No Default Action set for Listener forwarding traffic to Target Group. Invalid Configuration",
          LogLevel.ERROR);

      throw new WingsException(
          "No Default Action set for Listener forwarding traffic to Target Group. Invalid Configuration", USER)
          .addParam("message",
              "No Default Action set for Listener forwarding traffic to Target Group. Invalid Configuration");
    }

    return forwardAction.get().getTargetGroupArn();
  }

  private void createServiceForBlueGreen(EcsSetupParams setupParams, TaskDefinition taskDefinition,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    // Delete existing services except Green version of this service.
    ecsSetupCommandTaskHelper.deleteExistingServicesOtherThanBlueVersion(
        setupParams, cloudProviderSetting, encryptedDataDetails, executionLogCallback);

    String containerServiceName = ecsSetupCommandTaskHelper.createEcsService(setupParams, taskDefinition,
        cloudProviderSetting, encryptedDataDetails, commandExecutionDataBuilder, executionLogCallback);
    commandExecutionDataBuilder.targetGroupForNewService(setupParams.getTargetGroupArn());
    commandExecutionDataBuilder.containerServiceName(containerServiceName);

    ecsSetupCommandTaskHelper.storeCurrentServiceNameAndCountInfo((AwsConfig) cloudProviderSetting.getValue(),
        setupParams, encryptedDataDetails, commandExecutionDataBuilder, containerServiceName);

    ecsSetupCommandTaskHelper.backupAutoScalarConfig(setupParams, cloudProviderSetting, encryptedDataDetails,
        containerServiceName, commandExecutionDataBuilder, executionLogCallback);

    ecsSetupCommandTaskHelper.logLoadBalancerInfo(executionLogCallback, setupParams);
  }
}