package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.resources.Subscription;
import io.harness.azure.model.SubscriptionData;
import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.delegate.task.azure.request.AzureVMSSGetVirtualMachineScaleSetParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListResourceGroupsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVirtualMachineScaleSetsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSGetVirtualMachineScaleSetResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListResourceGroupsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListSubscriptionsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVirtualMachineScaleSetsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.exception.InvalidRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AzureConfig;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSSyncTaskHandler extends AzureVMSSTaskHandler {
  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig) {
    if (!azureVMSSTaskParameters.isSyncTask()) {
      throw new InvalidRequestException(format("Unrecognized object of class: [%s] while executing sync task",
          azureVMSSTaskParameters.getCommandType().name()));
    }

    AzureVMSSTaskResponse azureVMSSTaskResponse;
    switch (azureVMSSTaskParameters.getCommandType()) {
      case AZURE_VMSS_LIST_SUBSCRIPTIONS: {
        List<Subscription> subscriptions = azureVMSSHelperServiceDelegate.listSubscriptions(azureConfig);
        List<SubscriptionData> subscriptionsData =
            subscriptions.stream().map(toSubscriptionData()).collect(Collectors.toList());

        azureVMSSTaskResponse = AzureVMSSListSubscriptionsResponse.builder().subscriptions(subscriptionsData).build();
        break;
      }
      case AZURE_VMSS_LIST_RESOURCE_GROUPS_NAMES: {
        String subscriptionId =
            ((AzureVMSSListResourceGroupsNamesParameters) azureVMSSTaskParameters).getSubscriptionId();

        List<String> resourceGroupsNames =
            azureVMSSHelperServiceDelegate.listResourceGroupsNamesBySubscriptionId(azureConfig, subscriptionId);

        azureVMSSTaskResponse =
            AzureVMSSListResourceGroupsNamesResponse.builder().resourceGroupsNames(resourceGroupsNames).build();
        break;
      }
      case AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS: {
        String subscriptionId =
            ((AzureVMSSListVirtualMachineScaleSetsParameters) azureVMSSTaskParameters).getSubscriptionId();
        String resourceGroupName =
            ((AzureVMSSListVirtualMachineScaleSetsParameters) azureVMSSTaskParameters).getResourceGroupName();

        List<VirtualMachineScaleSet> virtualMachineScaleSets =
            azureVMSSHelperServiceDelegate.listVirtualMachineScaleSetsByResourceGroupName(
                azureConfig, subscriptionId, resourceGroupName);
        List<VirtualMachineScaleSetData> virtualMachineScaleSetsList =
            virtualMachineScaleSets.stream().map(toVirtualMachineScaleSetData()).collect(Collectors.toList());

        azureVMSSTaskResponse = AzureVMSSListVirtualMachineScaleSetsResponse.builder()
                                    .virtualMachineScaleSets(virtualMachineScaleSetsList)
                                    .build();
        break;
      }
      case AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET: {
        String subscriptionId =
            ((AzureVMSSGetVirtualMachineScaleSetParameters) azureVMSSTaskParameters).getSubscriptionId();
        String resourceGroupName =
            ((AzureVMSSGetVirtualMachineScaleSetParameters) azureVMSSTaskParameters).getResourceGroupName();
        String virtualMachineScaleSetName =
            ((AzureVMSSGetVirtualMachineScaleSetParameters) azureVMSSTaskParameters).getVmssName();

        Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
            azureVMSSHelperServiceDelegate.getVirtualMachineScaleSetByName(
                azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

        if (!virtualMachineScaleSetOp.isPresent()) {
          throw new InvalidRequestException(
              format("There is no Virtual Machine Scale Set with name %s, subscriptionId: %s, resourceGroupName: %s",
                  virtualMachineScaleSetName, subscriptionId, resourceGroupName));
        }

        VirtualMachineScaleSet virtualMachineScaleSet = virtualMachineScaleSetOp.get();

        String administratorUsername = virtualMachineScaleSet.virtualMachines()
                                           .list()
                                           .stream()
                                           .findFirst()
                                           .map(VirtualMachineScaleSetVM::administratorUserName)
                                           .orElse(EMPTY);

        VirtualMachineScaleSetData virtualMachineScaleSetData =
            VirtualMachineScaleSetData.builder()
                .id(virtualMachineScaleSet.id())
                .name(virtualMachineScaleSet.name())
                .virtualMachineAdministratorUsername(administratorUsername)
                .build();

        azureVMSSTaskResponse = AzureVMSSGetVirtualMachineScaleSetResponse.builder()
                                    .virtualMachineScaleSet(virtualMachineScaleSetData)
                                    .build();
        break;
      }
      case AZURE_VMSS_LIST_VM_DATA: {
        return null;
      }
      default: {
        throw new InvalidRequestException(format("Unrecognized object of class: [%s] while executing sync task",
            azureVMSSTaskParameters.getCommandType().name()));
      }
    }
    return AzureVMSSTaskExecutionResponse.builder()
        .azureVMSSTaskResponse(azureVMSSTaskResponse)
        .commandExecutionStatus(SUCCESS)
        .build();
  }

  @NotNull
  private Function<Subscription, SubscriptionData> toSubscriptionData() {
    return subscription
        -> SubscriptionData.builder().id(subscription.subscriptionId()).name(subscription.displayName()).build();
  }

  @NotNull
  private Function<VirtualMachineScaleSet, VirtualMachineScaleSetData> toVirtualMachineScaleSetData() {
    return vmss -> VirtualMachineScaleSetData.builder().id(vmss.id()).name(vmss.name()).build();
  }
}
