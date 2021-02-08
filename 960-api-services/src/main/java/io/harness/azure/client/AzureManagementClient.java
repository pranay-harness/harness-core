package io.harness.azure.client;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.management.ManagementGroupInfo;

import java.util.List;

public interface AzureManagementClient {
  /**
   *
   * List Resource Groups names by Subscription Id.
   *
   * @param azureConfig
   * @param subscriptionId
   * @return
   */
  List<String> listLocationsBySubscriptionId(AzureConfig azureConfig, String subscriptionId);

  /**
   *
   * List Management Group names.
   *
   * @param azureConfig
   * @return
   */
  List<ManagementGroupInfo> listManagementGroupNames(AzureConfig azureConfig);
}
