package io.harness.azure.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.management.ManagementGroupInfo;
import io.harness.azure.model.management.ManagementGroupListResult;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.Subscription;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Singleton
@Slf4j
public class AzureManagementClientImpl extends AzureClient implements AzureManagementClient {
  @Override
  public List<String> listLocationsBySubscriptionId(AzureConfig azureConfig, String subscriptionId) {
    Azure azure = isBlank(subscriptionId) ? getAzureClientWithDefaultSubscription(azureConfig)
                                          : getAzureClient(azureConfig, subscriptionId);

    log.debug("Start listing location by subscriptionId {}", subscriptionId);
    Subscription subscription =
        isBlank(subscriptionId) ? azure.getCurrentSubscription() : azure.subscriptions().getById(subscriptionId);

    return subscription != null
        ? subscription.listLocations().stream().map(Location::displayName).collect(Collectors.toList())
        : emptyList();
  }

  public List<ManagementGroupInfo> listManagementGroupNames(AzureConfig azureConfig) {
    log.debug("Start listing management groups");
    Response<ManagementGroupListResult> response;
    try {
      response = getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
                     .listManagementGroups(getAzureBearerAuthToken(azureConfig))
                     .execute();
    } catch (IOException e) {
      String errorMessage = "Error occurred while listing management groups";
      throw new InvalidRequestException(errorMessage, e);
    }

    if (response.isSuccessful()) {
      ManagementGroupListResult managementGroupListResult = response.body();
      return managementGroupListResult.getValue();
    } else {
      handleAzureErrorResponse(response.errorBody(), response.raw());
    }
    return Collections.emptyList();
  }
}
