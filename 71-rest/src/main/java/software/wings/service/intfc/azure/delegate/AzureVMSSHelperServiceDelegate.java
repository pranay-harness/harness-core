package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.resources.Subscription;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import software.wings.beans.AzureConfig;

import java.util.List;
import java.util.Optional;

public interface AzureVMSSHelperServiceDelegate {
  /**
   * Get Virtual Machine Scale Set by Id.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param virtuaMachineScaleSetId
   * @return
   */
  Optional<VirtualMachineScaleSet> getVirtualMachineScaleSetsById(
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
  Optional<VirtualMachineScaleSet> getVirtualMachineScaleSetByName(
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
   * Bulk delete Virtual Machine Scale Sets by Ids.
   *
   * @param azureConfig
   * @param vmssIds
   */
  void bulkDeleteVirtualMachineScaleSets(AzureConfig azureConfig, List<String> vmssIds);

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

  /**
   * Check if all VMSS Instances are stopped
   *
   * @param azureConfig
   * @param subscriptionId
   * @param virtualMachineScaleSetId
   * @param numberOfVMInstances
   * @return
   */
  boolean checkIsRequiredNumberOfVMInstances(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId, int numberOfVMInstances);

  /**
   * Update Virtual Machine Scale Set capacity
   *
   * @param azureConfig
   * @param virtualMachineScaleSetName
   * @param subscriptionId
   * @param resourceGroupName
   * @param limit
   * @return
   */
  VirtualMachineScaleSet updateVMSSCapacity(AzureConfig azureConfig, String virtualMachineScaleSetName,
      String subscriptionId, String resourceGroupName, int limit);

  /**
   * Create a new Virtual Machine Scale Set based on base scale set
   * @param azureConfig
   * @param baseVirtualMachineScaleSet
   * @param infraMappingId
   * @param newVirtualMachineScaleSetName
   * @param harnessRevision
   * @param azureUserAuthVMInstanceData
   * @param isBlueGreen
   */
  void createVirtualMachineScaleSet(AzureConfig azureConfig, VirtualMachineScaleSet baseVirtualMachineScaleSet,
      String infraMappingId, String newVirtualMachineScaleSetName, Integer harnessRevision,
      AzureUserAuthVMInstanceData azureUserAuthVMInstanceData, boolean isBlueGreen);
}
