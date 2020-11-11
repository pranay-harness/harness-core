package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppDeploymentSlotNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotNamesResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureWebAppListWebAppDeploymentSlotNamesTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureWebClient azureWebClient;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig) {
    String subscriptionId = azureAppServiceTaskParameters.getSubscriptionId();

    String resourceGroupName =
        ((AzureWebAppListWebAppDeploymentSlotNamesParameters) azureAppServiceTaskParameters).getResourceGroupName();
    String webAppName =
        ((AzureWebAppListWebAppDeploymentSlotNamesParameters) azureAppServiceTaskParameters).getAppName();

    List<DeploymentSlot> deploymentSlots =
        azureWebClient.listDeploymentSlotsByWebAppName(azureConfig, subscriptionId, resourceGroupName, webAppName);

    return AzureWebAppListWebAppDeploymentSlotNamesResponse.builder()
        .deploymentSlotNames(toDeploymentSlotNames(deploymentSlots))
        .build();
  }

  @NotNull
  private List<String> toDeploymentSlotNames(List<DeploymentSlot> deploymentSlots) {
    return deploymentSlots.stream().map(DeploymentSlot::name).collect(Collectors.toList());
  }
}
