/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
