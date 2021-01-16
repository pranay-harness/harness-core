package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;

import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Singleton;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureWebAppRollbackTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppRollbackParameters rollbackParameters = (AzureWebAppRollbackParameters) azureAppServiceTaskParameters;
    AzureAppServicePreDeploymentData preDeploymentData = rollbackParameters.getPreDeploymentData();
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext(rollbackParameters, azureConfig);
    Integer steadyTimeoutIntervalInMin = rollbackParameters.getTimeoutIntervalInMin();
    AzureAppServiceTaskType failedTaskType = preDeploymentData.getFailedTaskType();

    rollbackSetupSlot(preDeploymentData, azureWebClientContext, steadyTimeoutIntervalInMin, logStreamingTaskClient);

    if (AzureAppServiceTaskType.SLOT_SHIFT_TRAFFIC == failedTaskType
        || AzureAppServiceTaskType.SLOT_SWAP == failedTaskType) {
      rollbackUpdateSlotTrafficWeight(preDeploymentData, azureWebClientContext, logStreamingTaskClient);
    }

    List<AzureAppDeploymentData> azureAppDeploymentData = azureAppServiceDeploymentService.fetchDeploymentData(
        azureWebClientContext, rollbackParameters.getPreDeploymentData().getSlotName());

    markExecutionAsSuccess(azureAppServiceTaskParameters, logStreamingTaskClient);
    return AzureWebAppSlotSetupResponse.builder()
        .azureAppDeploymentData(azureAppDeploymentData)
        .preDeploymentData(preDeploymentData)
        .build();
  }

  private void rollbackSetupSlot(AzureAppServicePreDeploymentData preDeploymentData,
      AzureWebClientContext azureWebClientContext, Integer steadyTimeoutIntervalInMin,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureAppServiceDockerDeploymentContext dockerDeploymentContext = toAzureAppServiceDockerDeploymentContext(
        preDeploymentData, azureWebClientContext, steadyTimeoutIntervalInMin, logStreamingTaskClient);
    azureAppServiceDeploymentService.deployDockerImage(dockerDeploymentContext);
  }

  private AzureAppServiceDockerDeploymentContext toAzureAppServiceDockerDeploymentContext(
      AzureAppServicePreDeploymentData preDeploymentData, AzureWebClientContext azureWebClientContext,
      Integer steadyTimeoutIntervalInMin, ILogStreamingTaskClient logStreamingTaskClient) {
    return AzureAppServiceDockerDeploymentContext.builder()
        .logStreamingTaskClient(logStreamingTaskClient)
        .appSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(
            preDeploymentData.getAppSettingsToAdd()))
        .appSettingsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(
            preDeploymentData.getAppSettingsToRemove()))
        .connSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(
            preDeploymentData.getConnStringsToAdd()))
        .connSettingsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(
            preDeploymentData.getConnStringsToRemove()))
        .dockerSettings(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(
            preDeploymentData.getDockerSettingsToAdd()))
        .imagePathAndTag(preDeploymentData.getImageNameAndTag())
        .slotName(preDeploymentData.getSlotName())
        .azureWebClientContext(azureWebClientContext)
        .steadyStateTimeoutInMin(steadyTimeoutIntervalInMin)
        .build();
  }

  private void rollbackUpdateSlotTrafficWeight(AzureAppServicePreDeploymentData preDeploymentData,
      AzureWebClientContext azureWebClientContext, ILogStreamingTaskClient logStreamingTaskClient) {
    double trafficWeight = preDeploymentData.getTrafficWeight();
    String slotName = preDeploymentData.getSlotName();
    azureAppServiceDeploymentService.rerouteProductionSlotTraffic(
        azureWebClientContext, slotName, trafficWeight, logStreamingTaskClient);
  }
}
