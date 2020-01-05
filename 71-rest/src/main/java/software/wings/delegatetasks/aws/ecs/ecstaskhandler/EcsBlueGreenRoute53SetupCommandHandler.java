package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.TaskDefinition;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53ServiceSetupRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53ServiceSetupResponse;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;

import java.util.List;

@Singleton
public class EcsBlueGreenRoute53SetupCommandHandler extends EcsCommandTaskHandler {
  @Inject private EcsSetupCommandTaskHelper ecsSetupCommandTaskHelper;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    try {
      if (!(ecsCommandRequest instanceof EcsBGRoute53ServiceSetupRequest)) {
        return EcsCommandExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .ecsCommandResponse(EcsBGRoute53ServiceSetupResponse.builder()
                                    .output("Invalid Request Type: Expected was : [EcsBGRoute53ServiceSetupRequest]")
                                    .commandExecutionStatus(FAILURE)
                                    .build())
            .build();
      }

      EcsBGRoute53ServiceSetupRequest ecsBGRoute53ServiceSetupRequest =
          (EcsBGRoute53ServiceSetupRequest) ecsCommandRequest;
      EcsSetupParams setupParams = ecsBGRoute53ServiceSetupRequest.getEcsSetupParams();
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
          ContainerSetupCommandUnitExecutionData.builder();
      commandExecutionDataBuilder.ecsRegion(setupParams.getRegion());

      TaskDefinition taskDefinition =
          ecsSetupCommandTaskHelper.createTaskDefinition(ecsBGRoute53ServiceSetupRequest.getAwsConfig(),
              encryptedDataDetails, ecsBGRoute53ServiceSetupRequest.getServiceVariables(),
              ecsBGRoute53ServiceSetupRequest.getSafeDisplayServiceVariables(), executionLogCallback, setupParams);

      SettingAttribute cloudProviderSetting =
          aSettingAttribute().withValue(ecsBGRoute53ServiceSetupRequest.getAwsConfig()).build();

      ecsSetupCommandTaskHelper.deleteExistingServicesOtherThanBlueVersion(
          setupParams, cloudProviderSetting, encryptedDataDetails, executionLogCallback);

      String containerServiceName = ecsSetupCommandTaskHelper.createEcsService(setupParams, taskDefinition,
          cloudProviderSetting, encryptedDataDetails, commandExecutionDataBuilder, executionLogCallback);

      ecsSetupCommandTaskHelper.storeCurrentServiceNameAndCountInfo((AwsConfig) cloudProviderSetting.getValue(),
          setupParams, encryptedDataDetails, commandExecutionDataBuilder, containerServiceName);

      ecsSetupCommandTaskHelper.backupAutoScalarConfig(setupParams, cloudProviderSetting, encryptedDataDetails,
          containerServiceName, commandExecutionDataBuilder, executionLogCallback);

      ecsSetupCommandTaskHelper.logLoadBalancerInfo(executionLogCallback, setupParams);

      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(SUCCESS)
          .ecsCommandResponse(EcsBGRoute53ServiceSetupResponse.builder()
                                  .setupData(commandExecutionDataBuilder.build())
                                  .commandExecutionStatus(SUCCESS)
                                  .build())
          .build();
    } catch (Exception ex) {
      String errorMessage = getMessage(ex);
      executionLogCallback.saveExecutionLog(errorMessage, ERROR);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(errorMessage)
          .ecsCommandResponse(EcsBGRoute53ServiceSetupResponse.builder().commandExecutionStatus(FAILURE).build())
          .build();
    }
  }
}