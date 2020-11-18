package io.harness.azure.client;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.model.AzureConfig;

import java.util.Optional;

public interface AzureContainerRegistryClient {
  /**
   * Get container registry credentials.
   *
   * @param context
   * @return
   */
  Optional<RegistryCredentials> getContainerRegistryCredentials(AzureContainerRegistryClientContext context);

  /**
   * Filter registry by name on entire subscription. This is cost operation, try to avoid usage if it possible.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param registryName
   * @return
   */
  Optional<Registry> filterSubsriptionByContainerRegistryName(
      AzureConfig azureConfig, String subscriptionId, String registryName);
}
