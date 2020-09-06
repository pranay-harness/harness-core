package software.wings.service.impl.azure.delegate;

import static io.harness.azure.model.AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BACKEND_POOLS_LIST_EMPTY_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BASE_VIRTUAL_MACHINE_SCALE_SET_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BG_GREEN_TAG_VALUE;
import static io.harness.azure.model.AzureConstants.BG_VERSION_TAG_NAME;
import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.HARNESS_REVISION_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.NAME_TAG;
import static io.harness.azure.model.AzureConstants.NEW_VIRTUAL_MACHINE_SCALE_SET_NAME_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.NUMBER_OF_VM_INSTANCES_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.PRIMARY_INTERNET_FACING_LOAD_BALANCER_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VIRTUAL_MACHINE_SCALE_SET_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_DEFAULT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_SSH_PUBLIC_KEY;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.VMSS_IDS_IS_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VM_INSTANCE_IDS_LIST_EMPTY_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.VM_INSTANCE_IDS_NOT_NUMBERS_VALIDATION_MSG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.utils.AzureVMSSUtils.dateToISO8601BasicStr;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.utils.AsgConvention.getRevisionTagValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.SshConfiguration;
import com.microsoft.azure.management.compute.SshPublicKey;
import com.microsoft.azure.management.compute.UpgradeMode;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetOSProfile;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.compute.implementation.VirtualMachineScaleSetInner;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.VirtualMachineScaleSetNetworkInterface;
import com.microsoft.azure.management.network.implementation.PublicIPAddressInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import io.fabric8.utils.Objects;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureVMSSHelperServiceDelegate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  public List<VirtualMachineScaleSetVM> listVirtualMachineScaleSetVMs(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        getVirtualMachineScaleSetByName(azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);
    return virtualMachineScaleSetOp.map(this ::getVirtualMachineScaleSetVMs).orElse(Collections.emptyList());
  }

  @Override
  public List<VirtualMachineScaleSetVM> listVirtualMachineScaleSetVMs(
      AzureConfig azureConfig, String subscriptionId, String virtualMachineScaleSetId) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        getVirtualMachineScaleSetsById(azureConfig, subscriptionId, virtualMachineScaleSetId);
    if (!virtualMachineScaleSetOp.isPresent()) {
      return Collections.emptyList();
    }
    return virtualMachineScaleSetOp.map(this ::getVirtualMachineScaleSetVMs).orElse(Collections.emptyList());
  }

  @NotNull
  private List<VirtualMachineScaleSetVM> getVirtualMachineScaleSetVMs(VirtualMachineScaleSet virtualMachineScaleSet) {
    List<VirtualMachineScaleSetVM> virtualMachineScaleSetVMsList = new ArrayList<>();
    PagedList<VirtualMachineScaleSetVM> virtualMachineScaleSetVMs = virtualMachineScaleSet.virtualMachines().list();
    for (VirtualMachineScaleSetVM scaleSetVM : virtualMachineScaleSetVMs) {
      virtualMachineScaleSetVMsList.add(scaleSetVM);
    }
    return virtualMachineScaleSetVMsList;
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
    if (isEmpty(virtualMachineScaleSetName)) {
      return Optional.empty();
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
        baseVirtualMachineScaleSet, infraMappingId, harnessRevision, newVirtualMachineScaleSetName, isBlueGreen);

    VirtualMachineScaleSetInner inner = baseVirtualMachineScaleSet.inner();

    updateTags(inner, baseVMSSTags);
    updateUserData(inner, azureUserAuthVMInstanceData);
    updateCapacity(inner, 0);

    try {
      azure.virtualMachineScaleSets().inner().beginCreateOrUpdate(
          baseVirtualMachineScaleSet.resourceGroupName(), newVirtualMachineScaleSetName, inner);
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Error while creating virtual machine scale set, newVirtualMachineScaleSetName: %s, "
                  + "harnessRevision: %s, infraMappingId: %s",
              newVirtualMachineScaleSetName, harnessRevision, infraMappingId),
          e);
    }
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

  private void updateTags(VirtualMachineScaleSetInner inner, Map<String, String> baseVMSSTags) {
    inner.withTags(baseVMSSTags);
  }

  private void updateUserData(
      VirtualMachineScaleSetInner inner, AzureUserAuthVMInstanceData azureUserAuthVMInstanceData) {
    VirtualMachineScaleSetOSProfile osProfile = inner.virtualMachineProfile().osProfile();

    String userName = azureUserAuthVMInstanceData.getUserName();
    String rootUsername = isBlank(userName) ? osProfile.adminUsername() : userName;
    String vmssAuthType = azureUserAuthVMInstanceData.getVmssAuthType();
    osProfile.withAdminUsername(rootUsername);

    if (vmssAuthType.equals(VMSS_AUTH_TYPE_DEFAULT)) {
      osProfile.withAdminPassword(azureUserAuthVMInstanceData.getPassword());
    } else if (vmssAuthType.equals(VMSS_AUTH_TYPE_SSH_PUBLIC_KEY)) {
      String publicKeyData = azureUserAuthVMInstanceData.getSshPublicKey();
      osProfile.linuxConfiguration().withDisablePasswordAuthentication(true);

      SshConfiguration sshConfiguration = new SshConfiguration();
      sshConfiguration.withPublicKeys(new ArrayList<SshPublicKey>());
      osProfile.linuxConfiguration().withSsh(sshConfiguration);

      SshPublicKey sshPublicKey = new SshPublicKey();
      sshPublicKey.withKeyData(publicKeyData);
      sshPublicKey.withPath("/home/" + osProfile.adminUsername() + "/.ssh/authorized_keys");
      osProfile.linuxConfiguration().ssh().publicKeys().add(sshPublicKey);
    } else {
      throw new IllegalArgumentException(format("Unsupported Virtual machine Scale Set auth type %s", vmssAuthType));
    }
  }

  private void updateCapacity(VirtualMachineScaleSetInner inner, long capacity) {
    inner.sku().withCapacity(capacity);
  }

  @Override
  public VirtualMachineScaleSet attachVMSSToBackendPools(AzureConfig azureConfig,
      LoadBalancer primaryInternetFacingLoadBalancer, final String subscriptionId, final String resourceGroupName,
      final String virtualMachineScaleSetName, final String... backendPools) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        getVirtualMachineScaleSetByName(azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

    return virtualMachineScaleSetOp
        .map(vmss -> attachVMSSToBackendPools(azureConfig, vmss, primaryInternetFacingLoadBalancer, backendPools))
        .orElseThrow(()
                         -> new InvalidRequestException(
                             format("Virtual machine scale set cannot be found with virtualMachineScaleSetName: %s,"
                                     + "on subscriptionId: %s, resourceGroupName: %s",
                                 virtualMachineScaleSetName, subscriptionId, resourceGroupName)));
  }

  @Override
  public VirtualMachineScaleSet attachVMSSToBackendPools(AzureConfig azureConfig,
      VirtualMachineScaleSet virtualMachineScaleSet, LoadBalancer primaryInternetFacingLoadBalancer,
      final String... backendPools) {
    Objects.notNull(virtualMachineScaleSet, VIRTUAL_MACHINE_SCALE_SET_NULL_VALIDATION_MSG);
    Objects.notNull(primaryInternetFacingLoadBalancer, PRIMARY_INTERNET_FACING_LOAD_BALANCER_NULL_VALIDATION_MSG);
    if (backendPools.length == 0) {
      throw new IllegalArgumentException(BACKEND_POOLS_LIST_EMPTY_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start attaching virtual machine scale set with name {}, to backendPools: {}",
        virtualMachineScaleSet.name(), backendPools);
    return virtualMachineScaleSet.update()
        .withExistingPrimaryInternetFacingLoadBalancer(primaryInternetFacingLoadBalancer)
        .withPrimaryInternetFacingLoadBalancerBackends(backendPools)
        .apply();
  }

  @Override
  public VirtualMachineScaleSet detachVMSSFromBackendPools(AzureConfig azureConfig, final String subscriptionId,
      final String resourceGroupName, final String virtualMachineScaleSetName, final String... backendPools) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        getVirtualMachineScaleSetByName(azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

    return virtualMachineScaleSetOp.map(vmss -> detachVMSSFromBackendPools(azureConfig, vmss, backendPools))
        .orElseThrow(()
                         -> new InvalidRequestException(
                             format("Virtual machine scale set cannot be found with virtualMachineScaleSetName: %s,"
                                     + "on subscriptionId: %s, resourceGroupName: %s",
                                 virtualMachineScaleSetName, subscriptionId, resourceGroupName)));
  }

  @Override
  public VirtualMachineScaleSet detachVMSSFromBackendPools(
      AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet, final String... backendPools) {
    Objects.notNull(virtualMachineScaleSet, VIRTUAL_MACHINE_SCALE_SET_NULL_VALIDATION_MSG);
    if (backendPools.length == 0) {
      throw new IllegalArgumentException(BACKEND_POOLS_LIST_EMPTY_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start de-attaching virtual machine scale set with name {}, from backendPools: {}",
        virtualMachineScaleSet.name(), backendPools);
    return virtualMachineScaleSet.update().withoutPrimaryInternetFacingLoadBalancerBackends(backendPools).apply();
  }

  @Override
  public void updateVMInstances(VirtualMachineScaleSet virtualMachineScaleSet, final String... instanceIds) {
    if (instanceIds.length == 0) {
      throw new IllegalArgumentException(VM_INSTANCE_IDS_LIST_EMPTY_VALIDATION_MSG);
    }
    if (!validateVMInstanceIds(instanceIds)) {
      throw new IllegalArgumentException(VM_INSTANCE_IDS_NOT_NUMBERS_VALIDATION_MSG);
    }

    if (virtualMachineScaleSet.upgradeModel() == UpgradeMode.MANUAL) {
      logger.debug("Start updating VM instances of virtual machine scale set with name {}, instanceId: {}",
          virtualMachineScaleSet.name(), instanceIds);
      virtualMachineScaleSet.virtualMachines().updateInstances(instanceIds);
    }
  }

  private boolean validateVMInstanceIds(String[] instanceIds) {
    for (String id : instanceIds) {
      if (id.equals("*")) {
        continue;
      }
      try {
        Integer.parseInt(id);
      } catch (NumberFormatException e) {
        logger.error(
            format("Unable to convert VM instance id to numeric type, id: %s instanceIds: %s", id, instanceIds), e);
        return false;
      }
    }
    return true;
  }

  @Override
  public void forceDeAttachVMSSFromBackendPools(
      AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet, final String... backendPools) {
    VirtualMachineScaleSet deAttachedVMSS =
        detachVMSSFromBackendPools(azureConfig, virtualMachineScaleSet, backendPools);
    updateVMInstances(deAttachedVMSS, "*");
  }

  public void forceAttachVMSSToBackendPools(AzureConfig azureConfig, VirtualMachineScaleSet virtualMachineScaleSet,
      LoadBalancer primaryInternetFacingLoadBalancer, final String... backendPools) {
    VirtualMachineScaleSet attachedVMSS =
        attachVMSSToBackendPools(azureConfig, virtualMachineScaleSet, primaryInternetFacingLoadBalancer, backendPools);
    updateVMInstances(attachedVMSS, "*");
  }

  public Optional<PublicIPAddressInner> getVMPublicIPAddress(VirtualMachineScaleSetVM vm) {
    PagedList<VirtualMachineScaleSetNetworkInterface> vmScaleSetNetworkInterfaces = vm.listNetworkInterfaces();
    if (vmScaleSetNetworkInterfaces.isEmpty()) {
      return Optional.empty();
    }

    VirtualMachineScaleSetNetworkInterface virtualMachineScaleSetNetworkInterface = vmScaleSetNetworkInterfaces.get(0);
    PublicIPAddressInner publicIPAddressInner =
        virtualMachineScaleSetNetworkInterface.primaryIPConfiguration().inner().publicIPAddress();

    if (isNull(publicIPAddressInner)) {
      return Optional.empty();
    }

    return Optional.of(publicIPAddressInner);
  }

  public List<VirtualMachineScaleSetNetworkInterface> listVMVirtualMachineScaleSetNetworkInterfaces(
      VirtualMachineScaleSetVM vm) {
    PagedList<VirtualMachineScaleSetNetworkInterface> vmScaleSetNetworkInterfaces = vm.listNetworkInterfaces();
    if (vmScaleSetNetworkInterfaces.isEmpty()) {
      return Collections.emptyList();
    }

    List<VirtualMachineScaleSetNetworkInterface> vmScaleSetNetworkInterfacesList = new ArrayList<>();
    for (VirtualMachineScaleSetNetworkInterface vmScaleSetNetworkInterface : vmScaleSetNetworkInterfaces) {
      vmScaleSetNetworkInterfacesList.add(vmScaleSetNetworkInterface);
    }

    return vmScaleSetNetworkInterfacesList;
  }
}
