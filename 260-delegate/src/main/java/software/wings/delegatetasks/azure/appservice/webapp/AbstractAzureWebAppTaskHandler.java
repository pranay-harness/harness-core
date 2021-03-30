package software.wings.delegatetasks.azure.appservice.webapp;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.azure.appservice.AbstractAzureAppServiceTaskHandler;
import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentService;
import software.wings.delegatetasks.azure.common.AzureAppServiceService;

import com.google.inject.Inject;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public abstract class AbstractAzureWebAppTaskHandler extends AbstractAzureAppServiceTaskHandler {
  @Inject protected AzureAppServiceDeploymentService azureAppServiceDeploymentService;
  @Inject protected AzureAppServiceService azureAppServiceService;

  protected AzureWebClientContext buildAzureWebClientContext(
      AzureAppServiceTaskParameters appServiceTaskParameters, AzureConfig azureConfig) {
    return AzureWebClientContext.builder()
        .azureConfig(azureConfig)
        .appName(appServiceTaskParameters.getAppName())
        .subscriptionId(appServiceTaskParameters.getSubscriptionId())
        .resourceGroupName(appServiceTaskParameters.getResourceGroupName())
        .build();
  }

  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    throw new UnsupportedOperationException("Concrete subclass method implementation not available yet");
  }

  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient,
      ArtifactStreamAttributes artifactStreamAttributes) {
    throw new UnsupportedOperationException("Concrete subclass method implementation not available yet");
  }
}
