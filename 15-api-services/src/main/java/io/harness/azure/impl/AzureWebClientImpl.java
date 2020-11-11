package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_NULL_VALIDATION_MSG;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.model.AzureConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class AzureWebClientImpl extends AzureClient implements AzureWebClient {
  @Override
  public List<WebApp> listWebAppsByResourceGroupName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug("Start getting Web Applications by subscriptionId: {}, resourceGroupName: {}", subscriptionId,
        resourceGroupName);
    Instant startListingVMSS = Instant.now();
    PagedList<WebApp> webApps = azure.webApps().listByResourceGroup(resourceGroupName);

    List<WebApp> webAppsList = new ArrayList<>(webApps);
    long elapsedTime = Duration.between(startListingVMSS, Instant.now()).toMillis();
    log.info("Obtained Web Applications items: {} for elapsed time: {}, resourceGroupName: {}, subscriptionId: {} ",
        webAppsList.size(), elapsedTime, resourceGroupName, subscriptionId);

    return webAppsList;
  }

  @Override
  public List<DeploymentSlot> listDeploymentSlotsByWebAppName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String webAppName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(webAppName)) {
      throw new IllegalArgumentException(WEB_APP_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug("Start getting Deployment Slots by subscriptionId: {}, resourceGroupName: {}, webAppName: {}",
        subscriptionId, resourceGroupName, webAppName);
    PagedList<DeploymentSlot> deploymentSlots =
        azure.webApps().getByResourceGroup(resourceGroupName, webAppName).deploymentSlots().list();

    return new ArrayList<>(deploymentSlots);
  }
}
