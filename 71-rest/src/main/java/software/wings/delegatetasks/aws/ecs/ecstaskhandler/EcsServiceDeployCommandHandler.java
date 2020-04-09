package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.api.DeploymentType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;

import java.util.List;

@Deprecated
@Singleton
public class EcsServiceDeployCommandHandler extends EcsCommandTaskHandler {
  @Inject private Injector injector;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    try {
      if (!(ecsCommandRequest instanceof EcsServiceDeployRequest)) {
        return EcsCommandExecutionResponse.builder()
            .commandExecutionStatus(FAILURE)
            .ecsCommandResponse(EcsServiceDeployResponse.builder()
                                    .output("Invalid Request Type: Expected was : [EcsServiceDeployRequest]")
                                    .commandExecutionStatus(FAILURE)
                                    .build())
            .build();
      }

      EcsServiceDeployRequest request = (EcsServiceDeployRequest) ecsCommandRequest;
      ResizeCommandUnit resizeCommandUnit = new ResizeCommandUnit();
      resizeCommandUnit.setName("ECS Service Deploy");
      injector.injectMembers(resizeCommandUnit);
      SettingAttribute settingAttribute = aSettingAttribute().withValue(request.getAwsConfig()).build();
      CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                            .accountId(request.getAccountId())
                                                            .appId(request.getAppId())
                                                            .activityId(request.getActivityId())
                                                            .cloudProviderSetting(settingAttribute)
                                                            .cloudProviderCredentials(encryptedDataDetails)
                                                            .containerResizeParams(request.getEcsResizeParams())
                                                            .deploymentType(DeploymentType.ECS.name())
                                                            .build();
      CommandExecutionStatus status = resizeCommandUnit.execute(commandExecutionContext);
      ResizeCommandUnitExecutionData commandExecutionData =
          (ResizeCommandUnitExecutionData) commandExecutionContext.getCommandExecutionData();
      EcsServiceDeployResponse response = EcsServiceDeployResponse.builder()
                                              .commandExecutionStatus(status)
                                              .containerInfos(commandExecutionData.getContainerInfos())
                                              .oldInstanceData(commandExecutionData.getOldInstanceData())
                                              .newInstanceData(commandExecutionData.getNewInstanceData())
                                              .build();
      return EcsCommandExecutionResponse.builder().commandExecutionStatus(status).ecsCommandResponse(response).build();
    } catch (Exception ex) {
      String errorMessage = getMessage(ex);
      executionLogCallback.saveExecutionLog(errorMessage, ERROR);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(errorMessage)
          .ecsCommandResponse(EcsServiceDeployResponse.builder().commandExecutionStatus(FAILURE).build())
          .build();
    }
  }
}