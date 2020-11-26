package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppRollbackResponse;

import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
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

    setupSlot(preDeploymentData, azureWebClientContext, steadyTimeoutIntervalInMin, logStreamingTaskClient);
    updateSlotTrafficWeight(preDeploymentData, azureWebClientContext, logStreamingTaskClient);

    return AzureWebAppRollbackResponse.builder().build();
  }

  private void setupSlot(AzureAppServicePreDeploymentData preDeploymentData,
      AzureWebClientContext azureWebClientContext, Integer steadyTimeoutIntervalInMin,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureAppServiceDeploymentContext deploymentContext = toAzureAppServiceDeploymentContext(
        preDeploymentData, azureWebClientContext, steadyTimeoutIntervalInMin, logStreamingTaskClient);
    azureAppServiceDeploymentService.setupSlot(deploymentContext);
  }

  private AzureAppServiceDeploymentContext toAzureAppServiceDeploymentContext(
      AzureAppServicePreDeploymentData preDeploymentData, AzureWebClientContext azureWebClientContext,
      Integer steadyTimeoutIntervalInMin, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = new AzureAppServiceDeploymentContext();
    azureAppServiceDeploymentContext.setAzureWebClientContext(azureWebClientContext);
    azureAppServiceDeploymentContext.setLogStreamingTaskClient(logStreamingTaskClient);
    azureAppServiceDeploymentContext.setSlotName(preDeploymentData.getSlotName());
    azureAppServiceDeploymentContext.setAppSettingsToAdd(preDeploymentData.getAppSettingsToAdd());
    azureAppServiceDeploymentContext.setAppSettingsToRemove(preDeploymentData.getAppSettingsToRemove());
    azureAppServiceDeploymentContext.setConnSettingsToAdd(preDeploymentData.getConnSettingsToAdd());
    azureAppServiceDeploymentContext.setConnSettingsToRemove(preDeploymentData.getConnSettingsToRemove());
    azureAppServiceDeploymentContext.setSteadyStateTimeoutInMin(steadyTimeoutIntervalInMin);
    return azureAppServiceDeploymentContext;
  }

  private void updateSlotTrafficWeight(AzureAppServicePreDeploymentData preDeploymentData,
      AzureWebClientContext azureWebClientContext, ILogStreamingTaskClient logStreamingTaskClient) {
    double trafficWeight = preDeploymentData.getTrafficWeight();
    String slotName = preDeploymentData.getSlotName();
    azureAppServiceDeploymentService.rerouteProductionSlotTraffic(
        azureWebClientContext, slotName, trafficWeight, logStreamingTaskClient);
  }
}
