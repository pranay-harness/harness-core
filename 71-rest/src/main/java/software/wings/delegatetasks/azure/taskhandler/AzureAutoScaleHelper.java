package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_DESIRED_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MAX_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MIN_INSTANCES;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.monitor.AutoscaleProfile;
import io.harness.azure.client.AzureAutoScaleSettingsClient;
import io.harness.azure.client.AzureComputeClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureVMSSAutoScaleSettingsData;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureAutoScaleHelper {
  @Inject private AzureAutoScaleSettingsClient azureAutoScaleSettingsClient;
  @Inject private AzureComputeClient azureComputeClient;
  @Inject private DelegateLogService delegateLogService;

  public AzureVMSSAutoScaleSettingsData getVMSSAutoScaleInstanceLimits(AzureConfig azureConfig,
      AzureVMSSSetupTaskParameters setupTaskParameters, VirtualMachineScaleSet mostRecentActiveVMSS,
      boolean isUseCurrentRunningCount, String commandUnit) {
    ExecutionLogCallback logCallback = getLogCallBack(setupTaskParameters, commandUnit);

    if (isUseCurrentRunningCount) {
      if (mostRecentActiveVMSS != null) {
        Optional<AutoscaleProfile> defaultAutoScaleProfileOp = azureAutoScaleSettingsClient.getDefaultAutoScaleProfile(
            azureConfig, mostRecentActiveVMSS.resourceGroupName(), mostRecentActiveVMSS.id());
        if (defaultAutoScaleProfileOp.isPresent()) {
          return getMostRecentActiveVMSSInstanceLimits(
              defaultAutoScaleProfileOp.get(), mostRecentActiveVMSS, logCallback);
        }
      }
      return getDefaultVMSSInstanceLimits(logCallback);
    }

    return getWorkflowInputVMSSInstanceLimits(setupTaskParameters, logCallback);
  }

  private AzureVMSSAutoScaleSettingsData getMostRecentActiveVMSSInstanceLimits(AutoscaleProfile defaultAutoScaleProfile,
      VirtualMachineScaleSet mostRecentActiveVMSS, ExecutionLogCallback logCallback) {
    int minInstances = defaultAutoScaleProfile.minInstanceCount();
    int maxInstances = defaultAutoScaleProfile.maxInstanceCount();
    int desiredInstances = mostRecentActiveVMSS.capacity();
    String mostRecentActiveVMSSName = mostRecentActiveVMSS.name();

    logCallback.saveExecutionLog(format("Using currently running min: [%d], max: [%d], desired: [%d] from VMSS: [%s]",
                                     minInstances, maxInstances, desiredInstances, mostRecentActiveVMSSName),
        INFO);

    return AzureVMSSAutoScaleSettingsData.builder()
        .minInstances(minInstances)
        .maxInstances(maxInstances)
        .desiredInstances(desiredInstances)
        .build();
  }

  private AzureVMSSAutoScaleSettingsData getDefaultVMSSInstanceLimits(ExecutionLogCallback logCallback) {
    int minInstances = DEFAULT_AZURE_VMSS_MIN_INSTANCES;
    int maxInstances = DEFAULT_AZURE_VMSS_MAX_INSTANCES;
    int desiredInstances = DEFAULT_AZURE_VMSS_DESIRED_INSTANCES;

    logCallback.saveExecutionLog(
        format("Using default min: [%d], max: [%d], desired: [%d]", minInstances, maxInstances, desiredInstances),
        INFO);
    return AzureVMSSAutoScaleSettingsData.builder()
        .minInstances(minInstances)
        .maxInstances(maxInstances)
        .desiredInstances(desiredInstances)
        .build();
  }

  private AzureVMSSAutoScaleSettingsData getWorkflowInputVMSSInstanceLimits(
      AzureVMSSSetupTaskParameters setupTaskParameters, ExecutionLogCallback logCallback) {
    int minInstances = setupTaskParameters.getMinInstances();
    int maxInstances = setupTaskParameters.getMaxInstances();
    int desiredInstances = setupTaskParameters.getDesiredInstances();

    logCallback.saveExecutionLog(format("Using workflow input min: [%d], max: [%d] and desired: [%d]", minInstances,
                                     maxInstances, desiredInstances),
        INFO);

    return AzureVMSSAutoScaleSettingsData.builder()
        .minInstances(minInstances)
        .maxInstances(maxInstances)
        .desiredInstances(desiredInstances)
        .build();
  }

  public List<String> getVMSSAutoScaleSettingsJSONs(
      AzureConfig azureConfig, VirtualMachineScaleSet mostRecentActiveVMSS) {
    if (mostRecentActiveVMSS == null) {
      return emptyList();
    }

    String resourceGroupName = mostRecentActiveVMSS.resourceGroupName();
    return getAutoScaleSettingsJSONs(azureConfig, resourceGroupName, mostRecentActiveVMSS);
  }

  public List<String> getVMSSAutoScaleSettingsJSONs(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSet = azureComputeClient.getVirtualMachineScaleSetByName(
        azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

    return virtualMachineScaleSet.map(vm -> getAutoScaleSettingsJSONs(azureConfig, resourceGroupName, vm))
        .orElse(emptyList());
  }

  private List<String> getAutoScaleSettingsJSONs(
      AzureConfig azureConfig, String resourceGroupName, VirtualMachineScaleSet virtualMachineScaleSet) {
    return azureAutoScaleSettingsClient
        .getAutoScaleSettingJSONByTargetResourceId(azureConfig, resourceGroupName, virtualMachineScaleSet.id())
        .map(Collections::singletonList)
        .orElse(emptyList());
  }

  public ExecutionLogCallback getLogCallBack(AzureVMSSTaskParameters parameters, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, parameters.getAccountId(), parameters.getAppId(), parameters.getActivityId(), commandUnit);
  }
}
