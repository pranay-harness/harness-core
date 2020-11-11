package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.microsoft.azure.management.appservice.WebApp;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureWebAppListWebAppNamesTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureWebClient azureWebClient;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig) {
    String subscriptionId = azureAppServiceTaskParameters.getSubscriptionId();
    String resourceGroupName =
        ((AzureWebAppListWebAppNamesParameters) azureAppServiceTaskParameters).getResourceGroupName();

    List<WebApp> webApps =
        azureWebClient.listWebAppsByResourceGroupName(azureConfig, subscriptionId, resourceGroupName);

    return AzureWebAppListWebAppNamesResponse.builder().webAppNames(toWebAppNames(webApps)).build();
  }

  @NotNull
  private List<String> toWebAppNames(List<WebApp> webApps) {
    return webApps.stream().map(WebApp::name).collect(Collectors.toList());
  }
}
