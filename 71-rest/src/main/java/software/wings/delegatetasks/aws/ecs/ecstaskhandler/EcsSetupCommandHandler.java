package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceSetupResponse;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class EcsSetupCommandHandler extends EcsCommandTaskHandler {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EcsContainerService ecsContainerService;
  @Inject private EcsSetupCommandTaskHelper ecsSetupCommandTaskHelper;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    EcsServiceSetupResponse commandResponse =
        EcsServiceSetupResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;

    if (!(ecsCommandRequest instanceof EcsServiceSetupRequest)) {
      commandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      commandResponse.setOutput("Invalid Request Type: Expected was : EcsServiceSetupRequest");
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .ecsCommandResponse(commandResponse)
          .build();
    }

    try {
      EcsServiceSetupRequest ecsServiceSetupRequest = (EcsServiceSetupRequest) ecsCommandRequest;
      EcsSetupParams setupParams = ecsServiceSetupRequest.getEcsSetupParams();
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
          ContainerSetupCommandUnitExecutionData.builder();
      commandExecutionDataBuilder.ecsRegion(setupParams.getRegion());

      SettingAttribute cloudProviderSetting =
          aSettingAttribute().withValue(ecsServiceSetupRequest.getAwsConfig()).build();

      // Rollback Setup happens for ECS Daemon service, as for daemon scheduling strategy actual deployment happens in
      // setup state. In this case, we don't need to launch actual tasks, as ECS will schedule 1 task per instance in
      // cluster so only thing we need to do is, create taskDefinition and service.
      if (setupParams.isRollback()) {
        ecsSetupCommandTaskHelper.handleRollback(
            setupParams, cloudProviderSetting, commandExecutionDataBuilder, encryptedDataDetails, executionLogCallback);
      } else {
        // 1. Create new Task Definition
        TaskDefinition taskDefinition = ecsSetupCommandTaskHelper.createTaskDefinition(
            ecsServiceSetupRequest.getAwsConfig(), encryptedDataDetails, ecsServiceSetupRequest.getServiceVariables(),
            ecsServiceSetupRequest.getSafeDisplayServiceVariables(), executionLogCallback, setupParams);

        // 2. Create ECS Service
        if (!setupParams.isDaemonSchedulingStrategy()) {
          createServiceWithReplicaSchedulingStrategy(setupParams, taskDefinition, cloudProviderSetting,
              encryptedDataDetails, commandExecutionDataBuilder, executionLogCallback);
        } else {
          handleDaemonServiceRequest(setupParams, taskDefinition, executionLogCallback, cloudProviderSetting,
              encryptedDataDetails, commandExecutionDataBuilder);
        }
      }
      commandResponse.setSetupData(commandExecutionDataBuilder.build());
    } catch (Exception ex) {
      logger.error("Completed operation with errors");
      logger.error(ExceptionUtils.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);

      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      commandResponse.setCommandExecutionStatus(commandExecutionStatus);
      commandResponse.setOutput(ExceptionUtils.getMessage(ex));
    }

    return EcsCommandExecutionResponse.builder()
        .commandExecutionStatus(commandExecutionStatus)
        .ecsCommandResponse(commandResponse)
        .build();
  }

  private void createServiceWithReplicaSchedulingStrategy(EcsSetupParams setupParams, TaskDefinition taskDefinition,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    String containerServiceName = ecsSetupCommandTaskHelper.createEcsService(setupParams, taskDefinition,
        cloudProviderSetting, encryptedDataDetails, commandExecutionDataBuilder, executionLogCallback);
    commandExecutionDataBuilder.containerServiceName(containerServiceName);
    commandExecutionDataBuilder.targetGroupForNewService(setupParams.getTargetGroupArn());

    ecsSetupCommandTaskHelper.downsizeOldOrUnhealthy(
        cloudProviderSetting, setupParams, containerServiceName, encryptedDataDetails, executionLogCallback);

    ecsSetupCommandTaskHelper.cleanup(cloudProviderSetting, setupParams.getRegion(), containerServiceName,
        setupParams.getClusterName(), encryptedDataDetails, executionLogCallback);

    ecsSetupCommandTaskHelper.backupAutoScalarConfig(setupParams, cloudProviderSetting, encryptedDataDetails,
        containerServiceName, commandExecutionDataBuilder, executionLogCallback);

    ecsSetupCommandTaskHelper.logLoadBalancerInfo(executionLogCallback, setupParams);
  }

  private void handleDaemonServiceRequest(EcsSetupParams setupParams, TaskDefinition taskDefinition,
      ExecutionLogCallback executionLogCallback, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder) {
    // Get existing service. In case of Daemon service, we do not use versioning. We update existing service with new
    // task definition and some service configs mentioned in service spec
    Optional<Service> existingServiceMetadataSnapshot = ecsSetupCommandTaskHelper.getExistingServiceMetadataSnapshot(
        setupParams, cloudProviderSetting, encryptedDataDetails, setupParams.getTaskFamily(), awsHelperService);

    // We just use mapper to deserialize service Spec.We then use this object to get configs we want to updat e with
    // service
    CreateServiceRequest createServiceRequest =
        ecsSetupCommandTaskHelper.getCreateServiceRequest(cloudProviderSetting, encryptedDataDetails, setupParams,
            taskDefinition, setupParams.getTaskFamily(), executionLogCallback, logger, commandExecutionDataBuilder);

    if (existingServiceMetadataSnapshot.isPresent()) {
      Service service = existingServiceMetadataSnapshot.get();
      // Store existing service json spec, so it can be used if rollback is required
      commandExecutionDataBuilder
          .previousEcsServiceSnapshotJson(ecsSetupCommandTaskHelper.getJsonForAwsServiceConfig(service, logger))
          .containerServiceName(service.getServiceName())
          .ecsTaskDefintion(service.getTaskDefinition());
      commandExecutionDataBuilder.ecsServiceArn(service.getServiceArn());
      // Update existing service
      UpdateServiceRequest updateServiceRequest =
          new UpdateServiceRequest()
              .withService(createServiceRequest.getServiceName())
              .withCluster(setupParams.getClusterName())
              .withNetworkConfiguration(createServiceRequest.getNetworkConfiguration())
              .withTaskDefinition(createServiceRequest.getTaskDefinition())
              .withDeploymentConfiguration(createServiceRequest.getDeploymentConfiguration())
              .withHealthCheckGracePeriodSeconds(createServiceRequest.getHealthCheckGracePeriodSeconds());

      awsHelperService.updateService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
          encryptedDataDetails, updateServiceRequest);
    } else {
      CreateServiceResult createServiceResult = awsHelperService.createService(setupParams.getRegion(),
          (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails, createServiceRequest);
      commandExecutionDataBuilder.ecsServiceArn(createServiceResult.getService().getServiceArn())
          .containerServiceName(createServiceResult.getService().getServiceName())
          .ecsTaskDefintion(createServiceResult.getService().getTaskDefinition());
    }

    // Wait for all tasks to be up and service to reach steady state
    ecsContainerService.waitForDaemonServiceToReachSteadyState(setupParams.getRegion(), cloudProviderSetting,
        encryptedDataDetails, setupParams.getClusterName(), createServiceRequest.getServiceName(),
        setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
  }
}