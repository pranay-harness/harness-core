package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppRollbackResponse;

import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Singleton;
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
    AzureAppServiceTaskType failedStep = preDeploymentData.getFailedStep();

    rollbackSetupSlot(preDeploymentData, azureWebClientContext, steadyTimeoutIntervalInMin, logStreamingTaskClient);

    if (AzureAppServiceTaskType.SLOT_SHIFT_TRAFFIC == failedStep || AzureAppServiceTaskType.SLOT_SWAP == failedStep) {
      rollbackUpdateSlotTrafficWeight(preDeploymentData, azureWebClientContext, logStreamingTaskClient);
    }

    return AzureWebAppRollbackResponse.builder().build();
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
        .appSettingsToAdd(preDeploymentData.getAppSettingsToAdd())
        .appSettingsToRemove(preDeploymentData.getAppSettingsToRemove())
        .connSettingsToAdd(preDeploymentData.getConnSettingsToAdd())
        .connSettingsToRemove(preDeploymentData.getConnSettingsToRemove())
        .dockerSettings(preDeploymentData.getDockerSettingsToAdd())
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
