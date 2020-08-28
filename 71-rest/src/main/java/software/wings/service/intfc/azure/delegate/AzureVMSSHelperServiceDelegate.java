package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.network.LoadBalancer;
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
   * @param virtualMachineScaleSetId
   * @return
   */
  Optional<VirtualMachineScaleSet> getVirtualMachineScaleSetsById(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId);

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
   * List VMs of Virtual Machine Scale Set.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param virtualMachineScaleSetName
   * @return
   */
  List<VirtualMachineScaleSetVM> listVirtualMachineScaleSetVMs(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName);

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
   * Check if all VMSS Instances are stopped.
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
   * Update Virtual Machine Scale Set capacity.
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
   * Create a new Virtual Machine Scale Set based on base scale set.
   *
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

  /**
   * Attach virtual machine scale set to load balancer backend pools.
   *
   * @param azureConfig
   * @param primaryInternetFacingLoadBalancer
   * @param subscriptionId
   * @param resourceGroupName
   * @param virtualMachineScaleSetName
   * @param backendPools
   * @return
   */
  VirtualMachineScaleSet attachVMSSToBackendPools(AzureConfig azureConfig,
      LoadBalancer primaryInternetFacingLoadBalancer, String subscriptionId, String resourceGroupName,
      String virtualMachineScaleSetName, String... backendPools);

  /**
   * Attach virtual machine scale set to load balancer backend pools.
   *
   * @param azureConfig
   * @param virtualMachineScaleSet
   * @param primaryInternetFacingLoadBalancer
   * @param backendPools
   * @return
   */
  VirtualMachineScaleSet attachVMSSToBackendPools(AzureConfig azureConfig,
      VirtualMachineScaleSet virtualMachineScaleSet, LoadBalancer primaryInternetFacingLoadBalancer,
      String... backendPools);

  /**
   * De-attache virtual machine scale set from load balancer backend pools.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param virtualMachineScaleSetName
   * @param backendPools
   * @return
   */
  VirtualMachineScaleSet deAttachVMSSFromBackendPools(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String virtualMachineScaleSetName, String... backendPools);

  /**
   * De-attache virtual machine scale set from load balancer backend pools.
   *
   * @param azureConfig
   * @param virtualMachineScaleSet
   * @param backendPools
   * @return
   */
  VirtualMachineScaleSet deAttachVMSSFromBackendPools(
      AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet, String... backendPools);

  /**
   * Update virtual machine scale set VM instances to apply the latest network or configuration changes.
   *
   * @param virtualMachineScaleSet
   * @param instanceIds
   */
  void updateVMInstances(VirtualMachineScaleSet virtualMachineScaleSet, String... instanceIds);

  /**
   * Attach virtual machine scale set to load balancer backend pools and update VMSS VMs to apply network changes
   * immediately. This is time consuming operation.
   *
   * @param azureConfig
   * @param virtualMachineScaleSet
   * @param backendPools
   */
  void forceDeAttachVMSSFromBackendPools(
      AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet, String... backendPools);

  /**
   * De-attach virtual machine scale set from load balancer backend pools and update VMSS VMs to apply network changes
   * immediately. This is time consuming operation.
   *
   * @param azureConfig
   * @param virtualMachineScaleSet
   * @param primaryInternetFacingLoadBalancer
   * @param backendPools
   */
  void forceAttachVMSSToBackendPools(AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet,
      LoadBalancer primaryInternetFacingLoadBalancer, String... backendPools);
}
