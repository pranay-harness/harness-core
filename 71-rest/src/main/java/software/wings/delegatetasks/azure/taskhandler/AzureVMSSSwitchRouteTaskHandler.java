package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.AZURE_LOAD_BALANCER_DETAIL_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BG_BLUE_TAG_VALUE;
import static io.harness.azure.model.AzureConstants.BG_GREEN_TAG_VALUE;
import static io.harness.azure.model.AzureConstants.BG_ROLLBACK_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.BG_SWAP_ROUTES_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.BG_VERSION_TAG_NAME;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.NEW_VMSS_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.OLD_VMSS_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.network.LoadBalancer;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.request.AzureLoadBalancerDetailForBGDeployment;
import io.harness.delegate.task.azure.request.AzureVMSSSwitchRouteTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSSwitchRoutesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSSwitchRouteTaskHandler extends AzureVMSSTaskHandler {
  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig) {
    AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters =
        (AzureVMSSSwitchRouteTaskParameters) azureVMSSTaskParameters;
    validateTaskParams(switchRouteTaskParameters);

    String newVMSSName = switchRouteTaskParameters.getNewVMSSName();
    String oldVMSSName = switchRouteTaskParameters.getOldVMSSName();
    String loadBalancerName = switchRouteTaskParameters.getAzureLoadBalancerDetail().getLoadBalancerName();

    VirtualMachineScaleSet newVMSS = getVirtualMachineScaleSet(azureConfig, switchRouteTaskParameters, newVMSSName);
    VirtualMachineScaleSet oldVMSS = getVirtualMachineScaleSet(azureConfig, switchRouteTaskParameters, oldVMSSName);
    LoadBalancer loadBalancer = getLoadBalancer(azureConfig, switchRouteTaskParameters, loadBalancerName);

    if (switchRouteTaskParameters.isRollback()) {
      executeRollback(azureConfig, switchRouteTaskParameters, newVMSS, oldVMSS, loadBalancer);
    } else {
      executeSwapRoutes(azureConfig, switchRouteTaskParameters, newVMSS, oldVMSS, loadBalancer);
    }

    return buildAzureVMSSTaskExecutionResponse();
  }

  private void validateTaskParams(AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters) {
    String newVMSSName = switchRouteTaskParameters.getNewVMSSName();
    String oldVMSSName = switchRouteTaskParameters.getOldVMSSName();
    AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail =
        switchRouteTaskParameters.getAzureLoadBalancerDetail();

    if (isBlank(newVMSSName)) {
      throw new InvalidArgumentsException(NEW_VMSS_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(oldVMSSName)) {
      throw new InvalidArgumentsException(OLD_VMSS_NAME_NULL_VALIDATION_MSG);
    }
    if (isNull(azureLoadBalancerDetail)) {
      throw new InvalidArgumentsException(AZURE_LOAD_BALANCER_DETAIL_NULL_VALIDATION_MSG);
    }
  }

  private VirtualMachineScaleSet getVirtualMachineScaleSet(
      AzureConfig azureConfig, AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, String vmssName) {
    String subscriptionId = switchRouteTaskParameters.getSubscriptionId();
    String resourceGroupName = switchRouteTaskParameters.getResourceGroupName();

    Optional<VirtualMachineScaleSet> vmss =
        azureComputeClient.getVirtualMachineScaleSetByName(azureConfig, subscriptionId, resourceGroupName, vmssName);
    return vmss.orElseThrow(
        ()
            -> new InvalidRequestException(
                format("Not found virtual machine scale set with name: %s, subscriptionId: %s, resourceGroupName: %s",
                    vmssName, subscriptionId, resourceGroupName)));
  }

  private LoadBalancer getLoadBalancer(
      AzureConfig azureConfig, AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, String loadBalancerName) {
    String resourceGroupeName = switchRouteTaskParameters.getResourceGroupName();

    Optional<LoadBalancer> loadBalancerOp =
        azureNetworkClient.getLoadBalancerByName(azureConfig, resourceGroupeName, loadBalancerName);
    return loadBalancerOp.orElseThrow(
        ()
            -> new InvalidRequestException(format(
                "Not found load balancer with name: %s, resourceGroupName: %s", loadBalancerName, resourceGroupeName)));
  }

  private void executeSwapRoutes(AzureConfig azureConfig, AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters,
      VirtualMachineScaleSet newVMSS, VirtualMachineScaleSet oldVMSS, LoadBalancer loadBalancer) {
    ExecutionLogCallback bgSwapRoutesLogCallback =
        getLogCallBack(switchRouteTaskParameters, BG_SWAP_ROUTES_COMMAND_UNIT);
    executeSwapRoutesNewVMSS(azureConfig, switchRouteTaskParameters, newVMSS, loadBalancer, bgSwapRoutesLogCallback);
    executeSwapRoutesOldVMSS(azureConfig, oldVMSS, switchRouteTaskParameters, bgSwapRoutesLogCallback);
  }

  private void executeSwapRoutesNewVMSS(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet newVMSS,
      LoadBalancer loadBalancer, ExecutionLogCallback bgSwapRoutesLogCallback) {
    AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail =
        switchRouteTaskParameters.getAzureLoadBalancerDetail();
    int timeoutIntervalInMin = switchRouteTaskParameters.getTimeoutIntervalInMin();

    detachNewVMSSFromStageBackendPool(
        azureConfig, newVMSS, azureLoadBalancerDetail, timeoutIntervalInMin, bgSwapRoutesLogCallback);
    attachNewVMSSToProdBackendPool(
        azureConfig, newVMSS, loadBalancer, azureLoadBalancerDetail, timeoutIntervalInMin, bgSwapRoutesLogCallback);

    addTagsToVMSS(newVMSS, BG_VERSION_TAG_NAME, BG_BLUE_TAG_VALUE);
  }

  private void detachNewVMSSFromStageBackendPool(AzureConfig azureConfig, VirtualMachineScaleSet newVMSS,
      AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail, int timeoutIntervalInMin,
      ExecutionLogCallback bgSwapRoutesLogCallback) {
    String stageBackendPool = azureLoadBalancerDetail.getStageBackendPool();
    bgSwapRoutesLogCallback.saveExecutionLog(
        format("Sending request to detach blue virtual machine scale set:[%s] from stage backend pool:[%s]",
            newVMSS.name(), stageBackendPool));
    VirtualMachineScaleSet detachedVMSS =
        azureComputeClient.detachVMSSFromBackendPools(azureConfig, newVMSS, stageBackendPool);
    waitForUpdatingVMInstances(detachedVMSS, timeoutIntervalInMin, bgSwapRoutesLogCallback);
  }

  private void attachNewVMSSToProdBackendPool(AzureConfig azureConfig, VirtualMachineScaleSet newVMSS,
      LoadBalancer primaryInternetFacingLoadBalancer, AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail,
      int timeoutIntervalInMin, ExecutionLogCallback bgSwapRoutesLogCallback) {
    String prodBackendPool = azureLoadBalancerDetail.getProdBackendPool();
    bgSwapRoutesLogCallback.saveExecutionLog(
        format("Sending request to attach blue virtual machine scale set:[%s] to prod backend pool:[%s]",
            newVMSS.name(), prodBackendPool));
    VirtualMachineScaleSet attachedVMSS = azureComputeClient.attachVMSSToBackendPools(
        azureConfig, newVMSS, primaryInternetFacingLoadBalancer, prodBackendPool);
    waitForUpdatingVMInstances(attachedVMSS, timeoutIntervalInMin, bgSwapRoutesLogCallback);
  }

  private void executeSwapRoutesOldVMSS(AzureConfig azureConfig, VirtualMachineScaleSet oldVMSS,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, ExecutionLogCallback bgSwapRoutesLogCallback) {
    String resourceGroupName = switchRouteTaskParameters.getResourceGroupName();
    int timeoutIntervalInMin = switchRouteTaskParameters.getTimeoutIntervalInMin();
    AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail =
        switchRouteTaskParameters.getAzureLoadBalancerDetail();

    detachOldVMSSFromProdBackendPool(
        azureConfig, oldVMSS, azureLoadBalancerDetail, timeoutIntervalInMin, bgSwapRoutesLogCallback);
    clearAutoScalePolicyFromVMSS(azureConfig, oldVMSS, resourceGroupName);
    if (switchRouteTaskParameters.isDownscaleOldVMSS()) {
      scaleDownVMSSToZeroCapacityAndWait(azureConfig, switchRouteTaskParameters, oldVMSS);
    }
    addTagsToVMSS(oldVMSS, BG_VERSION_TAG_NAME, BG_GREEN_TAG_VALUE);
  }

  private void detachOldVMSSFromProdBackendPool(AzureConfig azureConfig, VirtualMachineScaleSet oldVMSS,
      AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail, int timeoutIntervalInMin,
      ExecutionLogCallback bgSwapRoutesLogCallback) {
    String prodBackendPool = azureLoadBalancerDetail.getProdBackendPool();
    bgSwapRoutesLogCallback.saveExecutionLog(
        format("Sending request to detach green virtual machine scale set:[%s] from prod backend pool:[%s]",
            oldVMSS.name(), prodBackendPool));
    VirtualMachineScaleSet detachedVMSS =
        azureComputeClient.detachVMSSFromBackendPools(azureConfig, oldVMSS, prodBackendPool);
    waitForUpdatingVMInstances(detachedVMSS, timeoutIntervalInMin, bgSwapRoutesLogCallback);
  }

  private void executeRollback(AzureConfig azureConfig, AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters,
      VirtualMachineScaleSet newVMSS, VirtualMachineScaleSet oldVMSS, LoadBalancer loadBalancer) {
    ExecutionLogCallback bgRollbackLogCallback = getLogCallBack(switchRouteTaskParameters, BG_ROLLBACK_COMMAND_UNIT);
    executeRollbackOldVMSS(azureConfig, switchRouteTaskParameters, oldVMSS, loadBalancer, bgRollbackLogCallback);
    executeRollbackNewVMSS(azureConfig, switchRouteTaskParameters, newVMSS, bgRollbackLogCallback);
  }

  private void executeRollbackOldVMSS(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet oldVMSS,
      LoadBalancer loadBalancer, ExecutionLogCallback bgRollbackLogCallback) {
    String resourceGroupName = switchRouteTaskParameters.getResourceGroupName();
    int timeoutIntervalInMin = switchRouteTaskParameters.getTimeoutIntervalInMin();
    AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail =
        switchRouteTaskParameters.getAzureLoadBalancerDetail();
    AzureVMSSPreDeploymentData preDeploymentData = switchRouteTaskParameters.getPreDeploymentData();
    String oldScalingPolicyJSON = EMPTY;
    if (!preDeploymentData.getScalingPolicyJSON().isEmpty()) {
      oldScalingPolicyJSON = preDeploymentData.getScalingPolicyJSON().get(0);
    }

    clearAutoScalePolicyFromVMSS(azureConfig, oldVMSS, resourceGroupName);
    scaleUpVMSSToDesiredCapacityAndWait(azureConfig, switchRouteTaskParameters, oldVMSS);
    attachOldVMSSToProdBackendPool(
        azureConfig, oldVMSS, loadBalancer, azureLoadBalancerDetail, timeoutIntervalInMin, bgRollbackLogCallback);
    attachAutoScalePolicyToVMSS(azureConfig, oldVMSS, resourceGroupName, oldScalingPolicyJSON);
    addTagsToVMSS(oldVMSS, BG_VERSION_TAG_NAME, BG_BLUE_TAG_VALUE);
  }

  private void attachOldVMSSToProdBackendPool(AzureConfig azureConfig, VirtualMachineScaleSet oldVMSS,
      LoadBalancer primaryInternetFacingLoadBalancer, AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail,
      int timeoutIntervalInMin, ExecutionLogCallback bgRollbackLogCallback) {
    String prodBackendPool = azureLoadBalancerDetail.getProdBackendPool();
    bgRollbackLogCallback.saveExecutionLog(
        format("Sending request to attach green virtual machine scale set:[%s] to prod backend pool:[%s]",
            oldVMSS.name(), prodBackendPool));
    VirtualMachineScaleSet attachedVMSS = azureComputeClient.attachVMSSToBackendPools(
        azureConfig, oldVMSS, primaryInternetFacingLoadBalancer, prodBackendPool);
    waitForUpdatingVMInstances(attachedVMSS, timeoutIntervalInMin, bgRollbackLogCallback);
  }

  private void executeRollbackNewVMSS(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet newVMSS,
      ExecutionLogCallback bgRollbackLogCallback) {
    String resourceGroupName = switchRouteTaskParameters.getResourceGroupName();
    int timeoutIntervalInMin = switchRouteTaskParameters.getTimeoutIntervalInMin();
    AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail =
        switchRouteTaskParameters.getAzureLoadBalancerDetail();

    detachNewVMSSFromProdBackendPool(
        azureConfig, newVMSS, azureLoadBalancerDetail, timeoutIntervalInMin, bgRollbackLogCallback);
    addTagsToVMSS(newVMSS, BG_VERSION_TAG_NAME, BG_GREEN_TAG_VALUE);
    clearAutoScalePolicyFromVMSS(azureConfig, newVMSS, resourceGroupName);
    scaleDownVMSSToZeroCapacityAndWait(azureConfig, switchRouteTaskParameters, newVMSS);
    deleteVMSS(azureConfig, newVMSS);
  }

  private void detachNewVMSSFromProdBackendPool(AzureConfig azureConfig, VirtualMachineScaleSet newVMSS,
      AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail, int timeoutIntervalInMin,
      ExecutionLogCallback bgRollbackLogCallback) {
    String prodBackendPool = azureLoadBalancerDetail.getProdBackendPool();
    bgRollbackLogCallback.saveExecutionLog(
        format("Sending request to detach blue virtual machine scale set:[%s] from prod backend pool:[%s]",
            newVMSS.name(), prodBackendPool));
    VirtualMachineScaleSet detachedVMSS =
        azureComputeClient.detachVMSSFromBackendPools(azureConfig, newVMSS, prodBackendPool);
    waitForUpdatingVMInstances(detachedVMSS, timeoutIntervalInMin, bgRollbackLogCallback);
  }

  private void waitForUpdatingVMInstances(
      VirtualMachineScaleSet vmss, int timeoutIntervalInMin, ExecutionLogCallback logCallback) {
    List<String> vmIds =
        vmss.virtualMachines().list().stream().map(VirtualMachineScaleSetVM::id).collect(Collectors.toList());

    try {
      timeLimiter.callWithTimeout(() -> {
        for (String vmId : vmIds) {
          logCallback.saveExecutionLog(
              format("Updating virtual machine instance: [%s] from the scale set: [%s]", vmId, vmss.name()));
          azureComputeClient.updateVMInstances(vmss, vmId);
        }

        logCallback.saveExecutionLog(
            format("All virtual machine instances updated from the scale set: [%s]", vmss.name()));
        return Boolean.TRUE;
      }, timeoutIntervalInMin, TimeUnit.MINUTES, true);
    } catch (Exception e) {
      throw new InvalidRequestException("Error while updating Virtual Machine Scale Set VM instances", e);
    }
  }

  private void scaleDownVMSSToZeroCapacityAndWait(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet vmss) {
    updateVMSSCapacityAndWait(azureConfig, switchRouteTaskParameters, vmss, 0, DOWN_SCALE_COMMAND_UNIT,
        DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
  }

  private void scaleUpVMSSToDesiredCapacityAndWait(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet vmss) {
    int desiredCapacity = switchRouteTaskParameters.getPreDeploymentData().getDesiredCapacity();

    updateVMSSCapacityAndWait(azureConfig, switchRouteTaskParameters, vmss, desiredCapacity, UP_SCALE_COMMAND_UNIT,
        UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
  }

  private void updateVMSSCapacityAndWait(AzureConfig azureConfig,
      AzureVMSSSwitchRouteTaskParameters switchRouteTaskParameters, VirtualMachineScaleSet vmss, int desiredCapacity,
      String scaleCommandUnit, String waitCommandUnit) {
    String subscriptionId = switchRouteTaskParameters.getSubscriptionId();
    String resourceGroupName = switchRouteTaskParameters.getResourceGroupName();
    Integer timeoutIntervalInMin = switchRouteTaskParameters.getTimeoutIntervalInMin();
    String vmssName = vmss.name();

    updateVMSSCapacityAndWaitForSteadyState(azureConfig, switchRouteTaskParameters, vmssName, subscriptionId,
        resourceGroupName, desiredCapacity, timeoutIntervalInMin, scaleCommandUnit, waitCommandUnit);
  }

  private void attachAutoScalePolicyToVMSS(
      AzureConfig azureConfig, VirtualMachineScaleSet vmss, String resourceGroupName, String scalePolicyJson) {
    String vmssId = vmss.id();
    azureAutoScaleSettingsClient.attachAutoScaleSettingToTargetResourceId(
        azureConfig, resourceGroupName, vmssId, scalePolicyJson);
  }

  private void clearAutoScalePolicyFromVMSS(
      AzureConfig azureConfig, VirtualMachineScaleSet vmss, String resourceGroupName) {
    String vmssId = vmss.id();
    azureAutoScaleSettingsClient.clearAutoScaleSettingOnTargetResourceId(azureConfig, resourceGroupName, vmssId);
  }

  private void addTagsToVMSS(VirtualMachineScaleSet vmss, String tagKey, String tagValue) {
    vmss.update().withTag(tagKey, tagValue).apply();
  }

  private void deleteVMSS(AzureConfig azureConfig, VirtualMachineScaleSet vmss) {
    String vmssId = vmss.id();
    azureComputeClient.deleteVirtualMachineScaleSetById(azureConfig, vmssId);
  }

  private AzureVMSSTaskExecutionResponse buildAzureVMSSTaskExecutionResponse() {
    AzureVMSSSwitchRoutesResponse switchRoutesTaskResponse = AzureVMSSSwitchRoutesResponse.builder().build();
    return AzureVMSSTaskExecutionResponse.builder()
        .azureVMSSTaskResponse(switchRoutesTaskResponse)
        .commandExecutionStatus(SUCCESS)
        .build();
  }
}
