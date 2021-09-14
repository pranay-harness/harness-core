/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
