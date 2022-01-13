/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.azure.manager;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.DeploymentSlotData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureAppServiceManager {
  List<String> getAppServiceNamesByResourceGroup(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String appId, String subscriptionId, String resourceGroup, String appType);

  List<DeploymentSlotData> getAppServiceDeploymentSlots(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String appId, String subscriptionId, String resourceGroup,
      String appType, String appName);

  List<AzureAppDeploymentData> listWebAppInstances(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String appId, String subscriptionId, String resourceGroupName,
      AzureAppServiceTaskParameters.AzureAppServiceType appType, String appName, String slotName);
}
