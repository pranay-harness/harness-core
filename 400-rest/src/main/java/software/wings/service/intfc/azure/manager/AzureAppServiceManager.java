package software.wings.service.intfc.azure.manager;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureAppServiceManager {
  List<String> getAppServiceNamesByResourceGroup(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String appId, String subscriptionId, String resourceGroup, String appType);

  List<String> getAppServiceDeploymentSlotNames(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String appId, String subscriptionId, String resourceGroup, String appType, String appName);

  List<AzureAppDeploymentData> listWebAppInstances(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String appId, String subscriptionId, String resourceGroupName,
      AzureAppServiceTaskParameters.AzureAppServiceType appType, String appName, String slotName);
}
