package software.wings.service.impl.azure.delegate;

import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import io.fabric8.utils.Objects;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureVMSSHelperServiceDelegate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureVMSSHelperServiceDelegateImpl extends AzureHelperService implements AzureVMSSHelperServiceDelegate {
  public static final String SUBSCRIPTION_ID_NULL_VALIDATION_MSG =
      "Parameter subscriptionId is required and cannot be null";
  public static final String RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG =
      "Parameter resourceGroupName is required and cannot be null";
  public static final String VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG =
      "Parameter virtualMachineScaleSetId is required and cannot be null";
  public static final String AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG = "Azure management client can't be null";
  public static final String VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG =
      "Parameter virtualScaleSetName is required and cannot be null";

  @Override
  public List<VirtualMachineScaleSet> listVirtualMachineScaleSetsByResourceGroupName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName) {
    Objects.notNull(resourceGroupName, RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    Objects.notNull(subscriptionId, SUBSCRIPTION_ID_NULL_VALIDATION_MSG);

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    List<VirtualMachineScaleSet> virtualMachineScaleSetsList = new ArrayList<>();

    logger.debug("Start getting Virtual Machine Scale Sets by resourceGroupName: {}, subscriptionId: {}",
        resourceGroupName, subscriptionId);
    Instant startListingVMSS = Instant.now();
    PagedList<VirtualMachineScaleSet> virtualMachineScaleSets =
        azure.virtualMachineScaleSets().listByResourceGroup(resourceGroupName);

    // Lazy listing https://github.com/Azure/azure-sdk-for-java/issues/860
    for (VirtualMachineScaleSet set : virtualMachineScaleSets) {
      virtualMachineScaleSetsList.add(set);
    }

    long elapsedTime = Duration.between(startListingVMSS, Instant.now()).toMillis();
    logger.info(
        "Obtained Virtual Machine Scale Sets items: {} for elapsed time: {}, resourceGroupName: {}, subscriptionId: {} ",
        virtualMachineScaleSetsList.size(), elapsedTime, resourceGroupName, subscriptionId);

    return virtualMachineScaleSetsList;
  }

  @Override
  public void deleteVirtualMachineScaleSetByResourceGroupName(
      AzureConfig azureConfig, String resourceGroupName, String virtualScaleSetName) {
    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Objects.notNull(resourceGroupName, RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    Objects.notNull(virtualScaleSetName, VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG);
    Objects.notNull(azure.virtualMachineScaleSets().getByResourceGroup(resourceGroupName, virtualScaleSetName),
        String.format("There is no virtual machine scale set with name %s", virtualScaleSetName));

    logger.debug("Start deleting Virtual Machine Scale Sets by resourceGroupName: {}", resourceGroupName);
    azure.virtualMachineScaleSets().deleteByResourceGroup(resourceGroupName, virtualScaleSetName);
  }

  @Override
  public void deleteVirtualMachineScaleSetById(AzureConfig azureConfig, String virtualMachineScaleSetId) {
    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Objects.notNull(virtualMachineScaleSetId, VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    Objects.notNull(azure.virtualMachineScaleSets().getById(virtualMachineScaleSetId),
        String.format(
            "There is no virtual machine scale set with virtualMachineScaleSetId %s", virtualMachineScaleSetId));

    logger.debug("Start deleting Virtual Machine Scale Sets by virtualMachineScaleSetId: {}", virtualMachineScaleSetId);
    azure.virtualMachineScaleSets().deleteById(virtualMachineScaleSetId);
  }

  @Override
  public VirtualMachineScaleSet getVirtualMachineScaleSetsById(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId) {
    Objects.notNull(virtualMachineScaleSetId, VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    Objects.notNull(subscriptionId, SUBSCRIPTION_ID_NULL_VALIDATION_MSG);

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start getting Virtual Machine Scale Sets by virtualMachineScaleSetId: {}, subscriptionId: {}",
        virtualMachineScaleSetId, subscriptionId);
    return azure.virtualMachineScaleSets().getById(virtualMachineScaleSetId);
  }

  @Override
  public VirtualMachineScaleSet getVirtualMachineScaleSetsByName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    Objects.notNull(virtualMachineScaleSetName, VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    Objects.notNull(subscriptionId, SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    Objects.notNull(resourceGroupName, RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug(
        "Start getting Virtual Machine Scale Sets name virtualMachineScaleSetName: {}, subscriptionId: {}, resourceGroupName: {}",
        virtualMachineScaleSetName, subscriptionId, resourceGroupName);
    return azure.virtualMachineScaleSets().getByResourceGroup(resourceGroupName, virtualMachineScaleSetName);
  }

  @Override
  public List<Subscription> listSubscriptions(AzureConfig azureConfig) {
    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing subscriptions for tenantId {}", azureConfig.getTenantId());
    PagedList<Subscription> subscriptions = azure.subscriptions().list();
    return subscriptions.stream().collect(Collectors.toList());
  }

  @Override
  public List<String> listResourceGroupsNamesBySubscriptionId(AzureConfig azureConfig, String subscriptionId) {
    Objects.notNull(subscriptionId, SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    Azure azure = getAzureClient(azureConfig, subscriptionId);

    logger.debug("Start listing resource groups names for subscriptionId {}", subscriptionId);
    List<ResourceGroup> resourceGroupList = azure.resourceGroups().list();
    return resourceGroupList.stream().map(HasName::name).collect(Collectors.toList());
  }
}
