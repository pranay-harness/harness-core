/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.ACR_REGISTRY_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.REGISTRY_HOST_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.REPOSITORY_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.client.AzureContainerRegistryRestClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.exception.InvalidRequestException;

import software.wings.helpers.ext.azure.AcrGetRepositoryTagsResponse;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

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

  @Override
  public List<Registry> listContainerRegistries(AzureConfig azureConfig, final String subscriptionId) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug("Start listing container registries on subscription, subscriptionId: {}", subscriptionId);
    return new ArrayList<>(azure.containerRegistries().list());
  }

  @Override
  public List<String> listRepositoryTags(
      AzureConfig azureConfig, final String registryHost, final String repositoryName) {
    if (isBlank(registryHost)) {
      throw new IllegalArgumentException(REGISTRY_HOST_BLANK_VALIDATION_MSG);
    }
    if (isBlank(repositoryName)) {
      throw new IllegalArgumentException(REPOSITORY_NAME_BLANK_VALIDATION_MSG);
    }

    AzureContainerRegistryRestClient azureContainerRegistryRestClient =
        getAzureContainerRegistryRestClient(registryHost);

    try {
      log.debug("Start listing repository tags, registryHost: {}, repositoryName: {}", registryHost, repositoryName);
      Response<AcrGetRepositoryTagsResponse> execute =
          azureContainerRegistryRestClient
              .listRepositoryTags(
                  getAzureBasicAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), repositoryName)
              .execute();

      if (execute.errorBody() != null) {
        throw new InvalidRequestException(
            format("Unable to list repository tags, registryHost: %s, repositoryName: %s, %s", registryHost,
                repositoryName, execute.errorBody().string()));
      }

      return execute.body().getTags();
    } catch (IOException e) {
      throw new InvalidRequestException(
          format("Unable to list repository tags, registryHost: %s, repositoryName: %s", registryHost, repositoryName),
          e);
    }
  }
}
