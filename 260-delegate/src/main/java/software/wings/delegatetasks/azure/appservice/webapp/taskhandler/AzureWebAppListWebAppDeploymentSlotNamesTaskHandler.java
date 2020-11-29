package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppDeploymentSlotNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotNamesResponse;

import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureWebAppListWebAppDeploymentSlotNamesTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureWebClient azureWebClient;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    String subscriptionId = azureAppServiceTaskParameters.getSubscriptionId();
    String resourceGroupName =
        ((AzureWebAppListWebAppDeploymentSlotNamesParameters) azureAppServiceTaskParameters).getResourceGroupName();
    String webAppName =
        ((AzureWebAppListWebAppDeploymentSlotNamesParameters) azureAppServiceTaskParameters).getAppName();

    AzureWebClientContext azureWebClientContext = AzureWebClientContext.builder()
                                                      .azureConfig(azureConfig)
                                                      .subscriptionId(subscriptionId)
                                                      .resourceGroupName(resourceGroupName)
                                                      .appName(webAppName)
                                                      .build();

    List<DeploymentSlot> deploymentSlots = azureWebClient.listDeploymentSlotsByWebAppName(azureWebClientContext);

    return AzureWebAppListWebAppDeploymentSlotNamesResponse.builder()
        .deploymentSlotNames(toDeploymentSlotNames(deploymentSlots))
        .build();
  }

  @NotNull
  private List<String> toDeploymentSlotNames(List<DeploymentSlot> deploymentSlots) {
    return deploymentSlots.stream().map(DeploymentSlot::name).collect(Collectors.toList());
  }
}
