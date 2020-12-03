package software.wings.delegatetasks.azure.appservice.webapp;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import software.wings.delegatetasks.azure.appservice.AbstractAzureAppServiceTaskHandler;
import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentService;

import com.google.inject.Inject;

public abstract class AbstractAzureWebAppTaskHandler extends AbstractAzureAppServiceTaskHandler {
  @Inject protected AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  protected AzureWebClientContext buildAzureWebClientContext(
      AzureAppServiceTaskParameters azureWebAppSlotSetupParameters, AzureConfig azureConfig) {
    return AzureWebClientContext.builder()
        .azureConfig(azureConfig)
        .appName(azureWebAppSlotSetupParameters.getAppName())
        .subscriptionId(azureWebAppSlotSetupParameters.getSubscriptionId())
        .resourceGroupName(azureWebAppSlotSetupParameters.getResourceGroupName())
        .build();
  }
}
