package io.harness.azure.client;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import io.harness.azure.model.AzureConfig;

import java.util.List;

public interface AzureWebClient {
  /**
   * List web application by resource group name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @return
   */
  List<WebApp> listWebAppsByResourceGroupName(AzureConfig azureConfig, String subscriptionId, String resourceGroupName);

  /**
   * List deployment slots by web application name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param webAppName
   * @return
   */
  List<DeploymentSlot> listDeploymentSlotsByWebAppName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String webAppName);
}
