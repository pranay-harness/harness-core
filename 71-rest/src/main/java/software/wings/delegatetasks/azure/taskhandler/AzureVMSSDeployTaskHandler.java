package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.SKIP_RESIZE_SCALE_SET;
import static io.harness.azure.model.AzureConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.monitor.ScaleCapacity;
import com.microsoft.azure.management.network.PublicIPAddressDnsSettings;
import com.microsoft.azure.management.network.VirtualMachineScaleSetNetworkInterface;
import com.microsoft.azure.management.network.implementation.PublicIPAddressInner;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;
import io.harness.delegate.task.azure.response.AzureVMSSDeployTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.ExceptionUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSDeployTaskHandler extends AzureVMSSTaskHandler {
  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      final AzureVMSSTaskParameters azureVMSSTaskParameters, final AzureConfig azureConfig) {
    AzureVMSSDeployTaskParameters deployTaskParameters = (AzureVMSSDeployTaskParameters) azureVMSSTaskParameters;
    try {
      AzureVMSSDeployTaskResponse deployTaskResponse = resizeVirtualMachineScaleSet(azureConfig, deployTaskParameters);
      return logAndGenerateSuccessResponse(deployTaskResponse, deployTaskParameters);
    } catch (Exception ex) {
      return logAndGenerateSFailureResponse(ex, deployTaskParameters);
    }
  }

  protected AzureVMSSResizeDetail getScaleSetDetailsForUpSizing(AzureVMSSDeployTaskParameters deployTaskParameters) {
    String scaleSetNameForUpSize = deployTaskParameters.getNewVirtualMachineScaleSetName();
    int currentDesiredCountForUpSize =
        deployTaskParameters.getNewDesiredCount() != null ? deployTaskParameters.getNewDesiredCount() : 0;
    List<String> baseScalingPolicyJSONs = deployTaskParameters.getBaseScalingPolicyJSONs();
    int desiredInstances = deployTaskParameters.getDesiredInstances();
    boolean attachScalingPolicy = currentDesiredCountForUpSize >= desiredInstances;

    return AzureVMSSResizeDetail.builder()
        .scaleSetName(scaleSetNameForUpSize)
        .desiredCount(currentDesiredCountForUpSize)
        .scalingPolicyJSONs(baseScalingPolicyJSONs)
        .attachScalingPolicy(attachScalingPolicy)
        .build();
  }

  protected AzureVMSSResizeDetail getScaleSetDetailsForDownSizing(AzureVMSSDeployTaskParameters deployTaskParameters) {
    String scaleSetNameForDownSize = deployTaskParameters.getOldVirtualMachineScaleSetName();
    int desiredCountForDownSize =
        deployTaskParameters.getOldDesiredCount() != null ? deployTaskParameters.getOldDesiredCount() : 0;
    List<String> baseScalingPolicyJSONs = deployTaskParameters.getPreDeploymentData().getScalingPolicyJSON();
    return AzureVMSSResizeDetail.builder()
        .scaleSetName(scaleSetNameForDownSize)
        .desiredCount(desiredCountForDownSize)
        .scalingPolicyJSONs(baseScalingPolicyJSONs)
        .attachScalingPolicy(false)
        .build();
  }

  protected Optional<VirtualMachineScaleSet> getScaleSet(
      AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters, String scaleSetName) {
    return azureComputeClient.getVirtualMachineScaleSetByName(azureConfig, deployTaskParameters.getSubscriptionId(),
        deployTaskParameters.getResourceGroupName(), scaleSetName);
  }

  private AzureVMSSDeployTaskResponse resizeVirtualMachineScaleSet(
      AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters) {
    if (deployTaskParameters.isResizeNewFirst()) {
      upSizeScaleSet(azureConfig, deployTaskParameters);
      downSizeScaleSet(azureConfig, deployTaskParameters);
    } else {
      downSizeScaleSet(azureConfig, deployTaskParameters);
      upSizeScaleSet(azureConfig, deployTaskParameters);
    }
    List<AzureVMInstanceData> instancesAdded =
        getInstances(azureConfig, deployTaskParameters.getNewVirtualMachineScaleSetName(), deployTaskParameters);
    List<AzureVMInstanceData> existingInstancesForOldScaleSet =
        getInstances(azureConfig, deployTaskParameters.getOldVirtualMachineScaleSetName(), deployTaskParameters);

    return AzureVMSSDeployTaskResponse.builder()
        .vmInstancesAdded(instancesAdded)
        .vmInstancesExisting(existingInstancesForOldScaleSet)
        .build();
  }

  private void upSizeScaleSet(AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters) {
    AzureVMSSResizeDetail scaleSetDetailsForUpSize = getScaleSetDetailsForUpSizing(deployTaskParameters);
    resizeScaleSet(azureConfig, deployTaskParameters, UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT,
        scaleSetDetailsForUpSize);
  }

  private void downSizeScaleSet(AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters) {
    AzureVMSSResizeDetail scaleSetDetailsForDownSizing = getScaleSetDetailsForDownSizing(deployTaskParameters);
    resizeScaleSet(azureConfig, deployTaskParameters, DOWN_SCALE_COMMAND_UNIT,
        DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT, scaleSetDetailsForDownSizing);
  }

  private void resizeScaleSet(AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters,
      String scaleCommandUnit, String waitCommandUnit, AzureVMSSResizeDetail azureVMSSResizeDetail) {
    String scaleSetName = azureVMSSResizeDetail.getScaleSetName();
    Optional<VirtualMachineScaleSet> scaleSet = getScaleSet(azureConfig, deployTaskParameters, scaleSetName);
    if (!scaleSet.isPresent()) {
      handleNonPresenceOfScaleSet(deployTaskParameters, scaleSetName, scaleCommandUnit, waitCommandUnit);
      return;
    }

    VirtualMachineScaleSet virtualMachineScaleSet = scaleSet.get();
    int desiredCount = azureVMSSResizeDetail.getDesiredCount();
    ExecutionLogCallback logCallBack = getLogCallBack(deployTaskParameters, scaleCommandUnit);

    clearScalingPolicy(azureConfig, virtualMachineScaleSet, deployTaskParameters, logCallBack);
    updateScaleSetCapacity(
        azureConfig, deployTaskParameters, scaleSetName, desiredCount, scaleCommandUnit, waitCommandUnit);
    attachScalingPolicy(azureConfig, virtualMachineScaleSet, deployTaskParameters, azureVMSSResizeDetail);
  }

  private void clearScalingPolicy(AzureConfig azureConfig, VirtualMachineScaleSet scaleSet,
      AzureVMSSDeployTaskParameters deployTaskParameters, ExecutionLogCallback logCallBack) {
    String scaleSetName = scaleSet.name();
    String scaleSetId = scaleSet.id();
    String resourceGroupName = deployTaskParameters.getResourceGroupName();
    logCallBack.saveExecutionLog(format("Clearing scaling policy for scale set = [%s]", scaleSetName));
    azureAutoScaleSettingsClient.clearAutoScaleSettingOnTargetResourceId(azureConfig, resourceGroupName, scaleSetId);
  }

  private void updateScaleSetCapacity(AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters,
      String scaleSetName, int capacity, String scaleCommandUnit, String waitCommandUnit) {
    String subscriptionId = deployTaskParameters.getSubscriptionId();
    String resourceGroupName = deployTaskParameters.getResourceGroupName();
    int timeoutIntervalInMin = deployTaskParameters.getTimeoutIntervalInMin() != null
        ? deployTaskParameters.getTimeoutIntervalInMin()
        : DEFAULT_AZURE_VMSS_TIMEOUT_MIN;

    updateVMSSCapacityAndWaitForSteadyState(azureConfig, deployTaskParameters, scaleSetName, subscriptionId,
        resourceGroupName, capacity, timeoutIntervalInMin, scaleCommandUnit, waitCommandUnit);
  }

  private void attachScalingPolicy(AzureConfig azureConfig, VirtualMachineScaleSet scaleSet,
      AzureVMSSDeployTaskParameters deployTaskParameters, AzureVMSSResizeDetail azureVMSSResizeDetail) {
    if (!azureVMSSResizeDetail.isAttachScalingPolicy()) {
      return;
    }

    String scaleSetId = scaleSet.id();
    String resourceGroupName = deployTaskParameters.getResourceGroupName();
    String minimum = String.valueOf(deployTaskParameters.getMinInstances());
    String maximum = String.valueOf(deployTaskParameters.getMaxInstances());
    String desired = String.valueOf(deployTaskParameters.getDesiredInstances());
    List<String> scalingPolicyJSONs = azureVMSSResizeDetail.getScalingPolicyJSONs();
    ScaleCapacity capacity = new ScaleCapacity();
    capacity.withMinimum(minimum).withMinimum(maximum).withDefaultProperty(desired);

    azureAutoScaleSettingsClient.attachAutoScaleSettingToTargetResourceId(
        azureConfig, resourceGroupName, scaleSetId, scalingPolicyJSONs, capacity);
  }

  private void handleNonPresenceOfScaleSet(AzureVMSSDeployTaskParameters deployTaskParameters, String scaleSetName,
      String scaleCommandUnit, String waitCommandUnit) {
    String message = format(SKIP_RESIZE_SCALE_SET, isEmpty(scaleSetName) ? EMPTY : scaleSetName);
    logger.warn(message);
    createAndFinishEmptyExecutionLog(deployTaskParameters, scaleCommandUnit, message);
    createAndFinishEmptyExecutionLog(deployTaskParameters, waitCommandUnit, message);
  }

  private List<AzureVMInstanceData> getInstances(
      AzureConfig azureConfig, String scaleSetName, AzureVMSSDeployTaskParameters deployTaskParameters) {
    List<VirtualMachineScaleSetVM> scaleSetVMs = azureComputeClient.listVirtualMachineScaleSetVMs(azureConfig,
        deployTaskParameters.getSubscriptionId(), deployTaskParameters.getResourceGroupName(), scaleSetName);

    return scaleSetVMs.stream().map(this ::generateVMInstanceData).collect(Collectors.toList());
  }

  private AzureVMInstanceData generateVMInstanceData(VirtualMachineScaleSetVM scaleSetVM) {
    String vmId = scaleSetVM.inner().vmId();
    String vmName = scaleSetVM.name();
    String privateIP = EMPTY;
    String publicDnsName = EMPTY;

    List<VirtualMachineScaleSetNetworkInterface> vmScaleSetNetworkInterfaces =
        azureComputeClient.listVMVirtualMachineScaleSetNetworkInterfaces(scaleSetVM);
    if (!vmScaleSetNetworkInterfaces.isEmpty()) {
      VirtualMachineScaleSetNetworkInterface virtualMachineScaleSetNetworkInterface =
          vmScaleSetNetworkInterfaces.get(0);
      privateIP = virtualMachineScaleSetNetworkInterface.primaryPrivateIP();

      PublicIPAddressInner publicIPAddressInner =
          virtualMachineScaleSetNetworkInterface.primaryIPConfiguration().inner().publicIPAddress();
      if (publicIPAddressInner != null) {
        PublicIPAddressDnsSettings publicIPAddressDnsSettings = publicIPAddressInner.dnsSettings();
        publicDnsName = publicIPAddressDnsSettings != null ? publicIPAddressDnsSettings.fqdn() : EMPTY;
      }
    }

    return AzureVMInstanceData.builder()
        .instanceId(vmId)
        .privateDnsName(vmName)
        .privateIpAddress(privateIP)
        .publicDnsName(publicDnsName)
        .build();
  }

  private AzureVMSSTaskExecutionResponse logAndGenerateSuccessResponse(
      AzureVMSSDeployTaskResponse deployTaskResponse, AzureVMSSDeployTaskParameters deployTaskParameters) {
    String newVirtualMachineScaleSetName = deployTaskParameters.getNewVirtualMachineScaleSetName();
    String oldVirtualMachineScaleSetName = deployTaskParameters.getOldVirtualMachineScaleSetName();

    int newScaleSetInstancesSize = deployTaskResponse.getVmInstancesAdded().size();
    int oldScaleSetInstancesSize = deployTaskResponse.getVmInstancesExisting().size();

    ExecutionLogCallback logCallBack = getLogCallBack(deployTaskParameters, deployTaskParameters.getCommandName());
    logCallBack.saveExecutionLog(
        format("Total number of new instances deployed for Scale Set = [%s] is [%d] ",
            isEmpty(newVirtualMachineScaleSetName) ? "" : newVirtualMachineScaleSetName, newScaleSetInstancesSize),
        INFO);
    logCallBack.saveExecutionLog(
        format("Total number of instances for old Scale Set [%s] is [%d]",
            isEmpty(oldVirtualMachineScaleSetName) ? "" : newVirtualMachineScaleSetName, oldScaleSetInstancesSize),
        INFO, SUCCESS);

    return AzureVMSSTaskExecutionResponse.builder()
        .azureVMSSTaskResponse(deployTaskResponse)
        .commandExecutionStatus(SUCCESS)
        .build();
  }

  private AzureVMSSTaskExecutionResponse logAndGenerateSFailureResponse(
      Exception ex, AzureVMSSDeployTaskParameters deployTaskParameters) {
    ExecutionLogCallback logCallBack = getLogCallBack(deployTaskParameters, deployTaskParameters.getCommandName());
    String errorMessage = ExceptionUtils.getMessage(ex);
    logCallBack.saveExecutionLog(format("Exception: [%s].", errorMessage), ERROR);
    logger.error(errorMessage, ex);
    return AzureVMSSTaskExecutionResponse.builder().errorMessage(errorMessage).commandExecutionStatus(FAILURE).build();
  }
}
