/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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

  /**
   * List load balancers for resource group based on resource group name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<String> listLoadBalancersNames(AzureConfig azureConfig, String subscriptionId, String resourceGroupName,
      List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * List backend pools names for Load Balancer based on load balancer name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param loadBalancerName
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<String> listLoadBalancerBackendPoolsNames(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String loadBalancerName, List<EncryptedDataDetail> encryptionDetails, String appId);

  /**
   * List Virtual Machines for VMSS based on id.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param vmssId
   * @param encryptionDetails
   * @param appId
   * @return
   */
  List<AzureVMData> listVMSSVirtualMachines(AzureConfig azureConfig, String subscriptionId, String resourceGroupName,
      String vmssId, List<EncryptedDataDetail> encryptionDetails, String appId);
}
