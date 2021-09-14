/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.azure.manager;

import io.harness.delegate.beans.azure.ManagementGroupData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureARMManager {
  /**
   * List subscription locations.
   *
   * @param azureConfig
   * @param encryptionDetails
   * @param appId
   * @param subscriptionId
   * @return
   */
  List<String> listSubscriptionLocations(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String appId, String subscriptionId);

  /**
   * List cloud provider default subscription locations.
   *
   * @param azureConfig
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<String> listAzureCloudProviderLocations(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * List management groups.
   *
   * @param azureConfig
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<ManagementGroupData> listManagementGroups(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
}
