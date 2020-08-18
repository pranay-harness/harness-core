package software.wings.service.intfc.azure.manager;

import io.harness.azure.model.AzureVMData;
import io.harness.azure.model.SubscriptionData;
import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureVMSSHelperServiceManager {
  /**
   * List subscriptions.
   *
   * @param azureConfig
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<SubscriptionData> listSubscriptions(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * List Resource Groups Names.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<String> listResourceGroupsNames(
      AzureConfig azureConfig, String subscriptionId, List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * List Virtual Machine Scale Sets.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<VirtualMachineScaleSetData> listVirtualMachineScaleSets(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * Get Virtual Machine Scale Set.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param vmssId
   * @param encryptionDetails
   * @param appId
   * @return
   */
  VirtualMachineScaleSetData getVirtualMachineScaleSet(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String vmssId, List<EncryptedDataDetail> encryptionDetails, String appId);

  List<AzureVMData> listVMSSVirtualMachines(AzureConfig azureConfig, String subscriptionId, String resourceGroupName,
      String vmssId, List<EncryptedDataDetail> encryptionDetails, String appId);
}
