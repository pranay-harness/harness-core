package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.resources.Subscription;
import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureVMSSHelperServiceDelegate {
  /**
   * Get Virtual Machine Scale Set by Id.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param virtuaMachineScaleSetId
   * @return
   */
  VirtualMachineScaleSet getVirtualMachineScaleSetsById(
      AzureConfig azureConfig, String subscriptionId, String virtuaMachineScaleSetId);

  /**
   * Get Virtual Machine Scale Set by name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param virtualMachineScaleSetName
   * @return
   */
  VirtualMachineScaleSet getVirtualMachineScaleSetsByName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName);

  /**
   * List Virtual Machine Scale Sets by Resource Group Name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @return
   */
  List<VirtualMachineScaleSet> listVirtualMachineScaleSetsByResourceGroupName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName);

  /**
   * Delete Virtual Machine Scale Set by Resource Group Name.
   *  @param azureConfig
   * @param resourceGroupName
   * @param virtualScaleSetName
   */
  void deleteVirtualMachineScaleSetByResourceGroupName(
      AzureConfig azureConfig, String resourceGroupName, String virtualScaleSetName);

  /**
   * Delete Virtual Machine Scale Set by Id.
   * @param azureConfig
   * @param virtualMachineScaleSetId
   */
  void deleteVirtualMachineScaleSetById(AzureConfig azureConfig, String virtualMachineScaleSetId);

  /**
   * List subscriptions.
   *
   * @param azureConfig
   * @return
   */
  List<Subscription> listSubscriptions(AzureConfig azureConfig);

  /**
   * List Resource Groups names by Subscription Id
   *
   * @param azureConfig
   * @param subscriptionId
   * @return
   */
  List<String> listResourceGroupsNamesBySubscriptionId(AzureConfig azureConfig, String subscriptionId);
}
