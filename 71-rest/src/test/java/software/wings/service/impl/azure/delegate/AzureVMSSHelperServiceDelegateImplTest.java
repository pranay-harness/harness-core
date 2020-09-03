package software.wings.service.impl.azure.delegate;

import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.NAME_TAG;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.TATHAGAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.google.common.util.concurrent.TimeLimiter;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.UpgradeMode;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.UpdateStages.WithApply;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.UpdateStages.WithPrimaryInternetFacingLoadBalancerBackendOrNatPool;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.UpdateStages.WithPrimaryInternetFacingLoadBalancerNatPool;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet.UpdateStages.WithPrimaryLoadBalancer;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVMs;
import com.microsoft.azure.management.compute.VirtualMachineScaleSets;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Subscriptions;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.RestException;
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureHelperService.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureVMSSHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks private AzureVMSSHelperServiceDelegateImpl azureVMSSHelperServiceDelegateImpl;

  @Before
  public void before() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");
    mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListSubscriptions() throws Exception {
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    Subscription subscription = mock(Subscription.class);
    Subscriptions subscriptions = mock(Subscriptions.class);
    PagedList<Subscription> pageList = getPageList();
    pageList.add(subscription);
    when(azure.subscriptions()).thenReturn(subscriptions);
    when(subscriptions.list()).thenReturn(pageList);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    List<Subscription> response = azureVMSSHelperServiceDelegateImpl.listSubscriptions(azureConfig);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListResourceGroupsNamesBySubscriptionId() throws Exception {
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);
    ResourceGroups resourceGroups = mock(ResourceGroups.class);
    ResourceGroup resourceGroup = mock(ResourceGroup.class);
    PagedList<ResourceGroup> pageList = getPageList();
    pageList.add(resourceGroup);
    when(azure.resourceGroups()).thenReturn(resourceGroups);
    when(resourceGroups.list()).thenReturn(pageList);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    List<String> response =
        azureVMSSHelperServiceDelegateImpl.listResourceGroupsNamesBySubscriptionId(azureConfig, "subscriptionId");

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetVirtualMachineScaleSetsById() throws Exception {
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);

    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);

    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.getById("virtualMachineSetId")).thenReturn(virtualMachineScaleSet);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    Optional<VirtualMachineScaleSet> response = azureVMSSHelperServiceDelegateImpl.getVirtualMachineScaleSetsById(
        azureConfig, "subscriptionId", "virtualMachineSetId");

    response.ifPresent(scaleSet -> assertThat(scaleSet).isNotNull());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetVirtualMachineScaleSetsByName() throws Exception {
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);
    VirtualMachineScaleSet virtualMachineScaleSet =
        mockVirtualMachineScaleSet("resourceGroupName", "virtualMachineScaleSetName");

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    Optional<VirtualMachineScaleSet> response = azureVMSSHelperServiceDelegateImpl.getVirtualMachineScaleSetByName(
        azureConfig, "subscriptionId", "resourceGroupName", "virtualMachineScaleSetName");

    response.ifPresent(vmm -> assertThat(vmm).isNotNull());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListVirtualMachineScaleSetsByResourceGroupName() throws Exception {
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);

    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);

    PagedList<VirtualMachineScaleSet> pageList = getPageList();
    pageList.add(virtualMachineScaleSet);

    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.listByResourceGroup("resourceGroupName")).thenReturn(pageList);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    List<VirtualMachineScaleSet> response =
        azureVMSSHelperServiceDelegateImpl.listVirtualMachineScaleSetsByResourceGroupName(
            azureConfig, "subscriptionId", "resourceGroupName");

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDeleteVirtualMachineScaleSetById() throws Exception {
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);

    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.getById("virtualMachineScaleSet")).thenReturn(virtualMachineScaleSet);
    doNothing().when(virtualMachineScaleSets).deleteById("virtualMachineScaleSet");

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    azureVMSSHelperServiceDelegateImpl.deleteVirtualMachineScaleSetById(azureConfig, "virtualMachineScaleSet");

    verify(azure, times(2)).virtualMachineScaleSets();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDeleteVirtualMachineScaleSetByResourceGroupName() throws Exception {
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);

    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.getByResourceGroup("resourceGroupName", "virtualMachineScaleSetName"))
        .thenReturn(virtualMachineScaleSet);

    doNothing().when(virtualMachineScaleSets).deleteByResourceGroup("resourceGroupName", "virtualMachineScaleSetName");

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    azureVMSSHelperServiceDelegateImpl.deleteVirtualMachineScaleSetByResourceGroupName(
        azureConfig, "resourceGroupName", "virtualMachineScaleSetName");

    verify(azure, times(2)).virtualMachineScaleSets();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testCheckIfAllVMSSInstancesAreInRunningState() throws Exception {
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);

    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSetVMs virtualMachineScaleSetVMs = mock(VirtualMachineScaleSetVMs.class);
    PagedList<VirtualMachineScaleSetVM> virtualMachineScaleSetVMPagedList = new PagedList<VirtualMachineScaleSetVM>() {
      @Override
      public Page<VirtualMachineScaleSetVM> nextPage(String s) throws RestException, IOException {
        return null;
      }
    };

    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.getById("virtualMachineSetId")).thenReturn(virtualMachineScaleSet);
    when(virtualMachineScaleSet.virtualMachines()).thenReturn(virtualMachineScaleSetVMs);
    when(virtualMachineScaleSetVMs.list()).thenReturn(virtualMachineScaleSetVMPagedList);
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    assertThat(azureVMSSHelperServiceDelegateImpl.checkIsRequiredNumberOfVMInstances(
                   azureConfig, "subscriptionId", "virtualMachineSetId", 0))
        .isTrue();
    assertThat(virtualMachineScaleSet.virtualMachines().list()).isNotNull();
    verify(azure, times(1)).virtualMachineScaleSets();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetTagsForNewVMSS() throws Exception {
    VirtualMachineScaleSet virtualMachineScaleSet = Mockito.mock(VirtualMachineScaleSet.class);
    Mockito.when(virtualMachineScaleSet.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__15");
        put("tag_name_1", "tag_value_1");
        put("tag_name_2", "tag_value_2");
      }
    });

    int harnessRevision = 6;
    boolean isBlueGreen = false;
    Map<String, String> result = azureVMSSHelperServiceDelegateImpl.getTagsForNewVMSS(
        virtualMachineScaleSet, "infraMappingId", harnessRevision, "newVirtualMachineScaleSetName", isBlueGreen);

    assertThat(result).isNotNull();
    assertThat(result.get("tag_name_1")).isEqualTo("tag_value_1");
    assertThat(result.get("tag_name_2")).isEqualTo("tag_value_2");
    assertThat(result.get(HARNESS_AUTOSCALING_GROUP_TAG_NAME)).isEqualTo("infraMappingId__6");
    assertThat(result.get(NAME_TAG)).isEqualTo("newVirtualMachineScaleSetName");
    assertThat(result.get(VMSS_CREATED_TIME_STAMP_TAG_NAME)).isNotNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testAttachVMSSToBackendPools() throws IOException {
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);

    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String virtualMachineScaleSetName = "virtualMachineScaleSetName";
    String backendPools = "backendPools";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    LoadBalancer primaryInternetFacingLoadBalancer = mock(LoadBalancer.class);
    VirtualMachineScaleSet virtualMachineScaleSet =
        mockVirtualMachineScaleSet(resourceGroupName, virtualMachineScaleSetName);
    WithPrimaryLoadBalancer withPrimaryLoadBalancer = mock(WithPrimaryLoadBalancer.class);
    WithPrimaryInternetFacingLoadBalancerBackendOrNatPool loadBalancerBackendOrNatPool =
        mock(WithPrimaryInternetFacingLoadBalancerBackendOrNatPool.class);
    WithPrimaryInternetFacingLoadBalancerNatPool loadBalancerNatPool =
        mock(WithPrimaryInternetFacingLoadBalancerNatPool.class);

    when(virtualMachineScaleSet.name()).thenReturn(virtualMachineScaleSetName);
    when(virtualMachineScaleSet.update()).thenReturn(withPrimaryLoadBalancer);
    when(withPrimaryLoadBalancer.withExistingPrimaryInternetFacingLoadBalancer(primaryInternetFacingLoadBalancer))
        .thenReturn(loadBalancerBackendOrNatPool);
    when(loadBalancerBackendOrNatPool.withPrimaryInternetFacingLoadBalancerBackends(backendPools))
        .thenReturn(loadBalancerNatPool);
    when(loadBalancerNatPool.apply()).thenReturn(virtualMachineScaleSet);

    VirtualMachineScaleSet response = azureVMSSHelperServiceDelegateImpl.attachVMSSToBackendPools(azureConfig,
        primaryInternetFacingLoadBalancer, subscriptionId, resourceGroupName, virtualMachineScaleSetName, backendPools);

    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDeAttachVMSSFromBackendPools() throws IOException {
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);

    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String virtualMachineScaleSetName = "virtualMachineScaleSetName";
    String backendPools = "backendPools";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    VirtualMachineScaleSet virtualMachineScaleSet =
        mockVirtualMachineScaleSet(resourceGroupName, virtualMachineScaleSetName);
    WithPrimaryLoadBalancer primaryLoadBalancer = mock(WithPrimaryLoadBalancer.class);
    WithApply withoutPrimaryLoadBalancerBackend = mock(WithApply.class);

    when(virtualMachineScaleSet.name()).thenReturn(virtualMachineScaleSetName);
    when(virtualMachineScaleSet.update()).thenReturn(primaryLoadBalancer);
    when(primaryLoadBalancer.withoutPrimaryInternetFacingLoadBalancerBackends(backendPools))
        .thenReturn(withoutPrimaryLoadBalancerBackend);
    when(withoutPrimaryLoadBalancerBackend.apply()).thenReturn(virtualMachineScaleSet);

    VirtualMachineScaleSet response = azureVMSSHelperServiceDelegateImpl.detachVMSSFromBackendPools(
        azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName, backendPools);

    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testUpdateVMInstances() {
    String instanceIds = "*";
    String virtualMachineScaleSetName = "virtualMachineScaleSetName";

    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSetVMs virtualMachines = mock(VirtualMachineScaleSetVMs.class);

    when(virtualMachineScaleSet.upgradeModel()).thenReturn(UpgradeMode.MANUAL);
    when(virtualMachineScaleSet.name()).thenReturn(virtualMachineScaleSetName);
    when(virtualMachineScaleSet.virtualMachines()).thenReturn(virtualMachines);
    doNothing().when(virtualMachines).updateInstances(instanceIds);

    azureVMSSHelperServiceDelegateImpl.updateVMInstances(virtualMachineScaleSet, instanceIds);

    verify(virtualMachines, times(1)).updateInstances(instanceIds);
  }

  public VirtualMachineScaleSet mockVirtualMachineScaleSet(
      String resourceGroupName, String virtualMachineScaleSetName) {
    VirtualMachineScaleSet virtualMachineScaleSet = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSets virtualMachineScaleSets = mock(VirtualMachineScaleSets.class);
    when(azure.virtualMachineScaleSets()).thenReturn(virtualMachineScaleSets);
    when(virtualMachineScaleSets.getByResourceGroup(resourceGroupName, virtualMachineScaleSetName))
        .thenReturn(virtualMachineScaleSet);
    return virtualMachineScaleSet;
  }

  @NotNull
  public <T> PagedList<T> getPageList() {
    return new PagedList<T>() {
      @Override
      public Page<T> nextPage(String s) {
        return new Page<T>() {
          @Override
          public String nextPageLink() {
            return null;
          }
          @Override
          public List<T> items() {
            return null;
          }
        };
      }
    };
  }
}
