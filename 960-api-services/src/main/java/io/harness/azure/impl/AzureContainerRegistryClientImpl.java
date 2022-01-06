/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.ACR_REGISTRY_NAME_BLANK_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.model.AzureConfig;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AzureContainerRegistryClientImpl extends AzureClient implements AzureContainerRegistryClient {
  @Override
  public Optional<RegistryCredentials> getContainerRegistryCredentials(AzureContainerRegistryClientContext context) {
    String registryName = context.getRegistryName();
    String resourceGroupName = context.getResourceGroupName();
    Azure azure = getAzureClientByContext(context);
    log.debug("Start getting container registry credentials by registryName: {}, context: {}", registryName, context);
    return Optional.ofNullable(azure.containerRegistries().getCredentials(resourceGroupName, registryName));
  }

  @Override
  public Optional<Registry> findFirstContainerRegistryByNameOnSubscription(
      AzureConfig azureConfig, String subscriptionId, String registryName) {
    if (isBlank(registryName)) {
      throw new IllegalArgumentException(ACR_REGISTRY_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Instant startFilteringRegistries = Instant.now();
    PagedList<Registry> registries = azure.containerRegistries().list();
    Optional<Registry> registryOptional =
        registries.stream().filter(registry -> registryName.equals(registry.name())).findFirst();
    long elapsedTime = Duration.between(startFilteringRegistries, Instant.now()).toMillis();
    log.info("Obtained container registry by name registryName: {} for elapsed time: {}, subscriptionId: {} ",
        registryName, elapsedTime, subscriptionId);
    return registryOptional;
  }
}
