/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.client;

import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.model.AzureConfig;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
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
   * Find registry by name on entire subscription. This is cost operation, try to avoid usage if it possible.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param registryName
   * @return
   */
  Optional<Registry> findFirstContainerRegistryByNameOnSubscription(
      AzureConfig azureConfig, String subscriptionId, String registryName);
}
