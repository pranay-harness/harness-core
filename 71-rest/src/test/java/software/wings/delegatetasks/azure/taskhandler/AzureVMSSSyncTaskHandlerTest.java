package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.azure.client.AzureComputeClient;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.azure.request.AzureVMSSGetVirtualMachineScaleSetParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListResourceGroupsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListSubscriptionsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVirtualMachineScaleSetsParameters;
import io.harness.delegate.task.azure.response.AzureVMSSGetVirtualMachineScaleSetResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListResourceGroupsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListSubscriptionsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVirtualMachineScaleSetsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DelegateLogService;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVMs;
import com.microsoft.azure.management.resources.Subscription;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AzureVMSSSyncTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AzureComputeClient mockAzureComputeClient;
  @Mock VirtualMachineScaleSetVMs virtualMachineScaleSetVMs;
  @Mock VirtualMachineScaleSetVM virtualMachineScaleSetVM;
  @Mock VirtualMachineScaleSet virtualMachineScaleSet;
  @Mock Subscription subscription;

  @Spy @InjectMocks AzureVMSSSyncTaskHandler handler;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListSubscriptions() throws Exception {
    doReturn(Collections.singletonList(subscription)).when(mockAzureComputeClient).listSubscriptions(any());

    AzureVMSSListSubscriptionsParameters azureVMSSListSubscriptionsParameters =
        AzureVMSSListSubscriptionsParameters.builder().build();
    AzureVMSSTaskExecutionResponse response =
        handler.executeTaskInternal(azureVMSSListSubscriptionsParameters, AzureConfig.builder().build());

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskExecutionResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskExecutionResponse).isNotNull();
    assertThat(azureVMSSTaskExecutionResponse instanceof AzureVMSSListSubscriptionsResponse).isTrue();

    AzureVMSSListSubscriptionsResponse listResourceGroupsNamesResponse =
        (AzureVMSSListSubscriptionsResponse) azureVMSSTaskExecutionResponse;
    assertThat(listResourceGroupsNamesResponse.getSubscriptions().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListResourceGroupsNames() throws Exception {
    List<String> resourceGroupList = Collections.singletonList("resourceGroup1, resourceGroup2");
    doReturn(resourceGroupList)
        .when(mockAzureComputeClient)
        .listResourceGroupsNamesBySubscriptionId(any(), anyString());

    AzureVMSSListResourceGroupsNamesParameters azureVMSSTaskParameters =
        AzureVMSSListResourceGroupsNamesParameters.builder().subscriptionId("subscriptionId").build();
    AzureVMSSTaskExecutionResponse response =
        handler.executeTaskInternal(azureVMSSTaskParameters, AzureConfig.builder().build());

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskExecutionResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskExecutionResponse).isNotNull();
    assertThat(azureVMSSTaskExecutionResponse instanceof AzureVMSSListResourceGroupsNamesResponse).isTrue();

    AzureVMSSListResourceGroupsNamesResponse listResourceGroupsNamesResponse =
        (AzureVMSSListResourceGroupsNamesResponse) azureVMSSTaskExecutionResponse;
    assertThat(listResourceGroupsNamesResponse.getResourceGroupsNames()).isEqualTo(resourceGroupList);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListVirtualMachineScaleSets() throws Exception {
    doReturn(Collections.singletonList(virtualMachineScaleSet))
        .when(mockAzureComputeClient)
        .listVirtualMachineScaleSetsByResourceGroupName(any(), anyString(), anyString());

    AzureVMSSListVirtualMachineScaleSetsParameters azureVMSSListVirtualMachineScaleSetsParameters =
        AzureVMSSListVirtualMachineScaleSetsParameters.builder().build();
    AzureVMSSTaskExecutionResponse response =
        handler.executeTaskInternal(azureVMSSListVirtualMachineScaleSetsParameters, AzureConfig.builder().build());

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskExecutionResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskExecutionResponse).isNotNull();
    assertThat(azureVMSSTaskExecutionResponse instanceof AzureVMSSListVirtualMachineScaleSetsResponse).isTrue();

    AzureVMSSListVirtualMachineScaleSetsResponse listVirtualMachineScaleSetsResponse =
        (AzureVMSSListVirtualMachineScaleSetsResponse) azureVMSSTaskExecutionResponse;
    assertThat(listVirtualMachineScaleSetsResponse.getVirtualMachineScaleSets().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetVirtualMachineScaleSets() throws Exception {
    doReturn(Optional.of(virtualMachineScaleSet))
        .when(mockAzureComputeClient)
        .getVirtualMachineScaleSetByName(any(), anyString(), anyString(), any());

    PagedList<VirtualMachineScaleSetVM> pageList = getPageList();
    pageList.add(virtualMachineScaleSetVM);
    doReturn(virtualMachineScaleSetVMs).when(virtualMachineScaleSet).virtualMachines();
    doReturn(pageList).when(virtualMachineScaleSetVMs).list();
    doReturn("administratorName").when(virtualMachineScaleSetVM).administratorUserName();

    AzureVMSSGetVirtualMachineScaleSetParameters azureVMSSGetVirtualMachineScaleSetParameters =
        AzureVMSSGetVirtualMachineScaleSetParameters.builder().build();
    AzureVMSSTaskExecutionResponse response =
        handler.executeTaskInternal(azureVMSSGetVirtualMachineScaleSetParameters, AzureConfig.builder().build());

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskExecutionResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskExecutionResponse).isNotNull();
    assertThat(azureVMSSTaskExecutionResponse instanceof AzureVMSSGetVirtualMachineScaleSetResponse).isTrue();

    AzureVMSSGetVirtualMachineScaleSetResponse getVirtualMachineScaleSetResponse =
        (AzureVMSSGetVirtualMachineScaleSetResponse) azureVMSSTaskExecutionResponse;
    assertThat(getVirtualMachineScaleSetResponse.getVirtualMachineScaleSet()).isNotNull();
    assertThat(getVirtualMachineScaleSetResponse.getVirtualMachineScaleSet().getVirtualMachineAdministratorUsername())
        .isEqualTo("administratorName");
  }

  @NotNull
  public PagedList<VirtualMachineScaleSetVM> getPageList() {
    return new PagedList<VirtualMachineScaleSetVM>() {
      @Override
      public Page<VirtualMachineScaleSetVM> nextPage(String s) {
        return new Page<VirtualMachineScaleSetVM>() {
          @Override
          public String nextPageLink() {
            return null;
          }
          @Override
          public List<VirtualMachineScaleSetVM> items() {
            return null;
          }
        };
      }
    };
  }
}
