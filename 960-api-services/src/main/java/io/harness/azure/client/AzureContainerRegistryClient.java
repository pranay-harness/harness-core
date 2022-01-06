/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
