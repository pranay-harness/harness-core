package software.wings.service.impl.azure.delegate;

import static io.harness.azure.model.AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BASE_VIRTUAL_MACHINE_SCALE_SET_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BG_GREEN_TAG_VALUE;
import static io.harness.azure.model.AzureConstants.BG_VERSION_TAG_NAME;
import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.HARNESS_REVISION_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.NAME_TAG;
import static io.harness.azure.model.AzureConstants.NEW_VIRTUAL_MACHINE_SCALE_SET_NAME_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.NUMBER_OF_VM_INSTANCES_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_DEFAULT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_SSH_PUBLIC_KEY;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.VMSS_IDS_IS_NULL_VALIDATION_MSG;
import static io.harness.delegate.task.utils.AzureVMSSUtils.dateToISO8601BasicStr;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.utils.AsgConvention.getRevisionTagValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.CachingTypes;
import com.microsoft.azure.management.compute.DataDisk;
import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.DefinitionStages.WithLinuxCreateManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.DefinitionStages.WithLinuxRootUsernameManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.DefinitionStages.WithManagedCreate;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.DefinitionStages.WithOS;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerInboundNatPool;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.implementation.SubnetInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.AvailabilityZoneId;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import io.fabric8.utils.Objects;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureVMSSHelperServiceDelegate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureVMSSHelperServiceDelegateImpl extends AzureHelperService implements AzureVMSSHelperServiceDelegate {
  @Override
  public List<VirtualMachineScaleSet> listVirtualMachineScaleSetsByResourceGroupName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }

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
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(virtualScaleSetName)) {
      throw new IllegalArgumentException(VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Objects.notNull(azure.virtualMachineScaleSets().getByResourceGroup(resourceGroupName, virtualScaleSetName),
        format("There is no virtual machine scale set with name %s", virtualScaleSetName));

    logger.debug("Start deleting Virtual Machine Scale Sets by resourceGroupName: {}", resourceGroupName);
    azure.virtualMachineScaleSets().deleteByResourceGroup(resourceGroupName, virtualScaleSetName);
  }

  @Override
  public void bulkDeleteVirtualMachineScaleSets(AzureConfig azureConfig, List<String> vmssIDs) {
    Objects.notNull(vmssIDs, VMSS_IDS_IS_NULL_VALIDATION_MSG);
    if (vmssIDs.isEmpty()) {
      return;
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start bulk deleting Virtual Machine Scale Sets, ids: {}", vmssIDs);
    azure.virtualMachineScaleSets().deleteByIds(vmssIDs);
  }

  @Override
  public void deleteVirtualMachineScaleSetById(AzureConfig azureConfig, String virtualMachineScaleSetId) {
    if (isBlank(virtualMachineScaleSetId)) {
      throw new IllegalArgumentException(VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Objects.notNull(azure.virtualMachineScaleSets().getById(virtualMachineScaleSetId),
        format("There is no virtual machine scale set with virtualMachineScaleSetId %s", virtualMachineScaleSetId));

    logger.debug("Start deleting Virtual Machine Scale Sets by virtualMachineScaleSetId: {}", virtualMachineScaleSetId);
    azure.virtualMachineScaleSets().deleteById(virtualMachineScaleSetId);
  }

  @Override
  public Optional<VirtualMachineScaleSet> getVirtualMachineScaleSetsById(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(virtualMachineScaleSetId)) {
      throw new IllegalArgumentException(VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start getting Virtual Machine Scale Sets by virtualMachineScaleSetId: {}, subscriptionId: {}",
        virtualMachineScaleSetId, subscriptionId);
    VirtualMachineScaleSet vmss = azure.virtualMachineScaleSets().getById(virtualMachineScaleSetId);

    return vmss == null ? Optional.empty() : Optional.of(vmss);
  }

  @Override
  public Optional<VirtualMachineScaleSet> getVirtualMachineScaleSetByName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(virtualMachineScaleSetName)) {
      throw new IllegalArgumentException(VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug(
        "Start getting Virtual Machine Scale Sets name virtualMachineScaleSetName: {}, subscriptionId: {}, resourceGroupName: {}",
        virtualMachineScaleSetName, subscriptionId, resourceGroupName);
    VirtualMachineScaleSet vmss =
        azure.virtualMachineScaleSets().getByResourceGroup(resourceGroupName, virtualMachineScaleSetName);

    return vmss == null ? Optional.empty() : Optional.of(vmss);
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
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing resource groups names for subscriptionId {}", subscriptionId);
    List<ResourceGroup> resourceGroupList = azure.resourceGroups().list();
    return resourceGroupList.stream().map(HasName::name).collect(Collectors.toList());
  }

  @Override
  public boolean checkIsRequiredNumberOfVMInstances(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId, int numberOfVMInstances) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(virtualMachineScaleSetId)) {
      throw new IllegalArgumentException(VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG);
    }
    if (numberOfVMInstances < 0) {
      throw new IllegalArgumentException(NUMBER_OF_VM_INSTANCES_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    VirtualMachineScaleSet virtualMachineScaleSet = azure.virtualMachineScaleSets().getById(virtualMachineScaleSetId);
    PagedList<VirtualMachineScaleSetVM> vmssInstanceList = virtualMachineScaleSet.virtualMachines().list();

    return (numberOfVMInstances == 0 ? vmssInstanceList.isEmpty() : vmssInstanceList.size() == numberOfVMInstances)
        || vmssInstanceList.stream().allMatch(
               instance -> instance.instanceView().statuses().get(0).displayStatus().equals("Provisioning succeeded"));
  }

  @Override
  public VirtualMachineScaleSet updateVMSSCapacity(AzureConfig azureConfig, String virtualMachineScaleSetName,
      String subscriptionId, String resourceGroupName, int newCapacity) {
    if (newCapacity < 0) {
      throw new IllegalArgumentException(format(
          "New VMSS capacity can't have negative value, virtualMachineScaleSetName: %s, subscriptionId: %s, resourceGroupName: %s,"
              + " newCapacity: %s",
          virtualMachineScaleSetName, subscriptionId, resourceGroupName, newCapacity));
    }
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        getVirtualMachineScaleSetByName(azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

    return virtualMachineScaleSetOp.map(vmss -> vmss.update().withCapacity(newCapacity).apply())
        .orElseThrow(
            ()
                -> new InvalidRequestException(format(
                    "There is no Virtual Machine Scale Set with name: %s, subscriptionId: %s, resourceGroupName: %s,"
                        + " newCapacity: %s",
                    virtualMachineScaleSetName, subscriptionId, resourceGroupName, newCapacity)));
  }

  @Override
  public void createVirtualMachineScaleSet(AzureConfig azureConfig, VirtualMachineScaleSet baseVirtualMachineScaleSet,
      String infraMappingId, String newVirtualMachineScaleSetName, Integer harnessRevision,
      AzureUserAuthVMInstanceData azureUserAuthVMInstanceData, boolean isBlueGreen) {
    if (isBlank(newVirtualMachineScaleSetName)) {
      throw new IllegalArgumentException(NEW_VIRTUAL_MACHINE_SCALE_SET_NAME_IS_NULL_VALIDATION_MSG);
    }
    Objects.notNull(baseVirtualMachineScaleSet, BASE_VIRTUAL_MACHINE_SCALE_SET_IS_NULL_VALIDATION_MSG);
    Objects.notNull(harnessRevision, HARNESS_REVISION_IS_NULL_VALIDATION_MSG);

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Map<String, String> baseVMSSTags = getTagsForNewVMSS(
        baseVirtualMachineScaleSet, infraMappingId, harnessRevision, newVirtualMachineScaleSetName, false);

    // Need for image reference and data disk
    VirtualMachineScaleSetVM virtualMachineScaleSetVM =
        baseVirtualMachineScaleSet.virtualMachines().list().stream().findFirst().orElseThrow(
            ()
                -> new InvalidRequestException(
                    format("Base VMSS doesn't have Virtual Machine, baseVirtualMachineScaleSetName: %s",
                        baseVirtualMachineScaleSet.name())));

    try {
      // Base VMSS Primary Network
      Network baseVMSSPrimaryNetwork = baseVirtualMachineScaleSet.getPrimaryNetwork();
      String subnetName = getSubnetName(baseVMSSPrimaryNetwork);

      // Base VMSS Primary Facing Load balancer and backend pools
      LoadBalancer primaryInternetFacingLoadBalancer =
          baseVirtualMachineScaleSet.getPrimaryInternetFacingLoadBalancer();
      String[] backendPools =
          new ArrayList<>(primaryInternetFacingLoadBalancer.backends().keySet()).toArray(new String[0]);

      // Inbound Nat Pool Names
      String[] inboundNatPoolNames =
          getNatPoolNames(baseVirtualMachineScaleSet.listPrimaryInternetFacingLoadBalancerInboundNatPools());

      WithOS vmssBuilder = azure.virtualMachineScaleSets()
                               .define(newVirtualMachineScaleSetName)
                               .withRegion(baseVirtualMachineScaleSet.regionName())
                               .withExistingResourceGroup(baseVirtualMachineScaleSet.resourceGroupName())
                               .withSku(baseVirtualMachineScaleSet.sku())
                               .withExistingPrimaryNetworkSubnet(baseVMSSPrimaryNetwork, subnetName)
                               .withExistingPrimaryInternetFacingLoadBalancer(primaryInternetFacingLoadBalancer)
                               .withPrimaryInternetFacingLoadBalancerBackends(backendPools)
                               .withPrimaryInternetFacingLoadBalancerInboundNatPools(inboundNatPoolNames)
                               .withoutPrimaryInternalLoadBalancer();

      // Image artifact will be provided by CDC team, until then getting the image from existing VM inside VMSS
      WithLinuxRootUsernameManagedOrUnmanaged vmssWithImageBuilder =
          setImageReference(vmssBuilder, virtualMachineScaleSetVM);

      WithLinuxCreateManagedOrUnmanaged vmssWithImageAndVMUserBuilder =
          setVMUserCredentials(vmssWithImageBuilder, baseVirtualMachineScaleSet, azureUserAuthVMInstanceData);

      WithManagedCreate vmssWithImageAndVMUserAndZoneBuilder =
          setAvailabilityZone(vmssWithImageAndVMUserBuilder, baseVirtualMachineScaleSet);

      WithManagedCreate vmssWithImageAndVMUserAndDataDiskBuilder =
          setDataDisk(vmssWithImageAndVMUserAndZoneBuilder, baseVirtualMachineScaleSet, virtualMachineScaleSetVM);

      // add tags and create new VMSS
      vmssWithImageAndVMUserAndDataDiskBuilder.withTags(baseVMSSTags).withCapacity(0).create();
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Error while creating virtual machine scale set, newVirtualMachineScaleSetName: %s, "
                  + "harnessRevision: %s, infraMappingId: %s",
              newVirtualMachineScaleSetName, harnessRevision, infraMappingId),
          e);
    }
  }

  public String getSubnetName(Network baseVMSSPrimaryNetwork) {
    return baseVMSSPrimaryNetwork.inner().subnets().stream().map(SubnetInner::name).findFirst().orElse(EMPTY);
  }

  @VisibleForTesting
  Map<String, String> getTagsForNewVMSS(VirtualMachineScaleSet baseVirtualMachineScaleSet, String infraMappingId,
      Integer harnessRevision, String newVirtualMachineScaleSetName, boolean isBlueGreen) {
    List<String> harnessTagsList = Arrays.asList(HARNESS_AUTOSCALING_GROUP_TAG_NAME, NAME_TAG);
    Map<String, String> baseVMSSTags = baseVirtualMachineScaleSet.tags()
                                           .entrySet()
                                           .stream()
                                           .filter(tagEntry -> !harnessTagsList.contains(tagEntry.getKey()))
                                           .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    baseVMSSTags.put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, getRevisionTagValue(infraMappingId, harnessRevision));
    baseVMSSTags.put(NAME_TAG, newVirtualMachineScaleSetName);
    baseVMSSTags.put(VMSS_CREATED_TIME_STAMP_TAG_NAME, dateToISO8601BasicStr(new Date()));

    if (isBlueGreen) {
      baseVMSSTags.put(BG_VERSION_TAG_NAME, BG_GREEN_TAG_VALUE);
    }

    return baseVMSSTags;
  }

  private WithLinuxRootUsernameManagedOrUnmanaged setImageReference(
      WithOS virtualMachineScaleSetsFluentBuilder, VirtualMachineScaleSetVM virtualMachineScaleSetVM) {
    ImageReference imageReference = virtualMachineScaleSetVM.platformImageReference();
    return virtualMachineScaleSetsFluentBuilder.withSpecificLinuxImageVersion(imageReference);
  }

  private WithLinuxCreateManagedOrUnmanaged setVMUserCredentials(
      WithLinuxRootUsernameManagedOrUnmanaged vmssWithImageReferenceBuilder,
      VirtualMachineScaleSet baseVirtualMachineScaleSet, AzureUserAuthVMInstanceData azureUserAuthVMInstanceData) {
    String userName = azureUserAuthVMInstanceData.getUserName();
    String rootUsername = isBlank(userName)
        ? baseVirtualMachineScaleSet.inner().virtualMachineProfile().osProfile().adminUsername()
        : userName;
    String vmssAuthType = azureUserAuthVMInstanceData.getVmssAuthType();
    if (vmssAuthType.equals(VMSS_AUTH_TYPE_DEFAULT)) {
      return vmssWithImageReferenceBuilder.withRootUsername(rootUsername)
          .withRootPassword(azureUserAuthVMInstanceData.getPassword());
    } else if (vmssAuthType.equals(VMSS_AUTH_TYPE_SSH_PUBLIC_KEY)) {
      return vmssWithImageReferenceBuilder.withRootUsername(rootUsername)
          .withSsh(azureUserAuthVMInstanceData.getSshPublicKey());
    } else {
      throw new InvalidRequestException(format("Unsupported Virtual machine Scale Set auth type %s", vmssAuthType));
    }
  }

  private WithManagedCreate setAvailabilityZone(WithLinuxCreateManagedOrUnmanaged vmssWithImageAndVMUserBuilder,
      VirtualMachineScaleSet baseVirtualMachineScaleSet) {
    Set<AvailabilityZoneId> availabilityZones = baseVirtualMachineScaleSet.availabilityZones();
    AvailabilityZoneId availabilityZoneId = availabilityZones.iterator().next();
    return vmssWithImageAndVMUserBuilder.withAvailabilityZone(availabilityZoneId);
  }

  private WithManagedCreate setDataDisk(WithManagedCreate vmssWithImageReferenceAndVMUserBuilder,
      VirtualMachineScaleSet baseVirtualMachineScaleSet, VirtualMachineScaleSetVM virtualMachineScaleSetVM) {
    DataDisk dataDisk = virtualMachineScaleSetVM.storageProfile().dataDisks().get(0);
    CachingTypes cachingType = dataDisk.caching();
    Integer diskSizeGB = dataDisk.diskSizeGB();
    int lun = dataDisk.lun();
    // Storage Account Types
    StorageAccountTypes storageAccountType = baseVirtualMachineScaleSet.managedOSDiskStorageAccountType();
    return vmssWithImageReferenceAndVMUserBuilder.withNewDataDisk(diskSizeGB, lun, cachingType, storageAccountType);
  }

  private String[] getNatPoolNames(Map<String, LoadBalancerInboundNatPool> loadBalancerInboundNatPoolMap) {
    List<String> natPoolNames = new ArrayList<>();
    for (LoadBalancerInboundNatPool natPool : loadBalancerInboundNatPoolMap.values()) {
      natPoolNames.add(natPool.name());
    }
    return natPoolNames.toArray(new String[0]);
  }
}
