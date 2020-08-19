package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_DESIRED_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MAX_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MIN_INSTANCES;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.NUMBER_OF_LATEST_VERSIONS_TO_KEEP;
import static io.harness.azure.model.AzureConstants.SETUP_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_DEFAULT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_SSH_PUBLIC_KEY;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.utils.AzureVMSSUtils.iso8601BasicStrToDate;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.utils.AsgConvention.getAsgName;
import static software.wings.utils.AsgConvention.getRevisionFromTag;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.monitor.AutoscaleProfile;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.azure.model.AzureVMSSAutoScalingData;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AzureConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSSetupTaskHandler extends AzureVMSSTaskHandler {
  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig) {
    if (!(azureVMSSTaskParameters instanceof AzureVMSSSetupTaskParameters)) {
      String message = format("Parameters of unrecognized class: [%s] found while executing setup step.",
          azureVMSSTaskParameters.getClass().getSimpleName());
      logger.error(message);
      return AzureVMSSTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    AzureVMSSSetupTaskParameters setupTaskParameters = (AzureVMSSSetupTaskParameters) azureVMSSTaskParameters;
    ExecutionLogCallback logCallback = getLogCallBack(azureVMSSTaskParameters, SETUP_COMMAND_UNIT);

    logCallback.saveExecutionLog("Starting Azure Virtual Machine Scale Set Setup", INFO);

    logCallback.saveExecutionLog("Getting all Harness managed Virtual Machine Scale Sets", INFO);
    List<VirtualMachineScaleSet> harnessVMSSSortedByCreationTimeReverse =
        getHarnessMangedVMSSSortedByCreationTime(azureConfig, setupTaskParameters);

    logCallback.saveExecutionLog("Getting the most recent active Virtual Machine Scale Set", INFO);
    VirtualMachineScaleSet mostRecentActiveVMSS = getMostRecentActiveVMSS(harnessVMSSSortedByCreationTimeReverse);

    downsizeOrDeleteOlderVirtualMachineScaleSets(
        azureConfig, setupTaskParameters, harnessVMSSSortedByCreationTimeReverse, mostRecentActiveVMSS, logCallback);

    logCallback.saveExecutionLog("Getting the new revision and Virtual Machine Scale Set name", INFO);
    Integer newHarnessRevision = getNewHarnessRevision(harnessVMSSSortedByCreationTimeReverse);
    String newVirtualMachineScaleSetName = getNewVirtualMachineScaleSetName(setupTaskParameters, newHarnessRevision);

    createVirtualMachineScaleSet(
        azureConfig, setupTaskParameters, newHarnessRevision, newVirtualMachineScaleSetName, logCallback);

    AzureVMSSSetupTaskResponse azureVMSSSetupTaskResponse = buildAzureVMSSSetupTaskResponse(azureConfig,
        newHarnessRevision, newVirtualMachineScaleSetName, mostRecentActiveVMSS, setupTaskParameters, logCallback);

    logCallback.saveExecutionLog(
        format("Completed Azure VMSS Setup with new scale set name [%s]", newVirtualMachineScaleSetName), INFO,
        CommandExecutionStatus.SUCCESS);

    return AzureVMSSTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .azureVMSSTaskResponse(azureVMSSSetupTaskResponse)
        .build();
  }

  private List<VirtualMachineScaleSet> getHarnessMangedVMSSSortedByCreationTime(
      AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters) {
    List<VirtualMachineScaleSet> harnessManagedVMSS = listAllHarnessManagedVMSS(azureConfig, setupTaskParameters);
    return sortVMSSByCreationDate(harnessManagedVMSS);
  }

  @VisibleForTesting
  private List<VirtualMachineScaleSet> listAllHarnessManagedVMSS(
      AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters) {
    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();
    String infraMappingId = setupTaskParameters.getInfraMappingId();

    List<VirtualMachineScaleSet> virtualMachineScaleSets =
        azureVMSSHelperServiceDelegate.listVirtualMachineScaleSetsByResourceGroupName(
            azureConfig, subscriptionId, resourceGroupName);

    return virtualMachineScaleSets.stream().filter(isHarnessManagedScaleSet(infraMappingId)).collect(toList());
  }

  private Predicate<VirtualMachineScaleSet> isHarnessManagedScaleSet(final String infraMappingId) {
    return scaleSet
        -> scaleSet.tags().entrySet().stream().anyMatch(tagEntry -> isHarnessManagedTag(tagEntry, infraMappingId));
  }

  private boolean isHarnessManagedTag(Map.Entry<String, String> tagEntry, final String infraMappingId) {
    return tagEntry.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG_NAME)
        && tagEntry.getValue().startsWith(infraMappingId);
  }

  private List<VirtualMachineScaleSet> sortVMSSByCreationDate(List<VirtualMachineScaleSet> harnessManagedVMSS) {
    Comparator<VirtualMachineScaleSet> createdTimeComparator = (vmss1, vmss2) -> {
      Date createdTimeVMss1 = iso8601BasicStrToDate(vmss1.tags().get(VMSS_CREATED_TIME_STAMP_TAG_NAME));
      Date createdTimeVMss2 = iso8601BasicStrToDate(vmss2.tags().get(VMSS_CREATED_TIME_STAMP_TAG_NAME));
      return createdTimeVMss2.compareTo(createdTimeVMss1);
    };
    return harnessManagedVMSS.stream().sorted(createdTimeComparator).collect(Collectors.toList());
  }

  private VirtualMachineScaleSet getMostRecentActiveVMSS(List<VirtualMachineScaleSet> harnessManagedScaleSets) {
    Optional<VirtualMachineScaleSet> harnessManagedScaleSetsWithNonZeroCount =
        harnessManagedScaleSets.stream()
            .filter(Objects::nonNull)
            .filter(scaleSet -> scaleSet.capacity() > 0)
            .findFirst();

    return harnessManagedScaleSetsWithNonZeroCount.orElseGet(
        () -> harnessManagedScaleSets.stream().findFirst().orElse(null));
  }

  private void downsizeOrDeleteOlderVirtualMachineScaleSets(AzureConfig azureConfig,
      AzureVMSSSetupTaskParameters setupTaskParameters, List<VirtualMachineScaleSet> sortedHarnessManagedVMSSs,
      VirtualMachineScaleSet mostRecentActiveVMSS, ExecutionLogCallback executionLogCallback) {
    if (isEmpty(sortedHarnessManagedVMSSs) || mostRecentActiveVMSS == null) {
      return;
    }

    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();
    int autoScalingSteadyStateVMSSTimeout = setupTaskParameters.getAutoScalingSteadyStateVMSSTimeout();
    String mostRecentActiveVMSSName = mostRecentActiveVMSS.name();
    int versionsToKeep = NUMBER_OF_LATEST_VERSIONS_TO_KEEP - 1;

    List<VirtualMachineScaleSet> listOfVMSSsForDownsize =
        sortedHarnessManagedVMSSs.stream()
            .filter(Objects::nonNull)
            .filter(vmss -> !vmss.name().equals(mostRecentActiveVMSSName))
            .limit(versionsToKeep)
            .collect(toList());
    List<VirtualMachineScaleSet> listOfVMSSsForDelete =
        sortedHarnessManagedVMSSs.stream()
            .filter(Objects::nonNull)
            .filter(vmss -> !vmss.name().equals(mostRecentActiveVMSSName))
            .skip(versionsToKeep)
            .collect(toList());

    downsizeVMSSs(azureConfig, setupTaskParameters, listOfVMSSsForDownsize, subscriptionId, resourceGroupName,
        autoScalingSteadyStateVMSSTimeout, executionLogCallback);
    deleteVMSSs(azureConfig, listOfVMSSsForDelete, executionLogCallback);
  }

  public void downsizeVMSSs(AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters,
      List<VirtualMachineScaleSet> vmssForDownsize, String subscriptionId, String resourceGroupName,
      int autoScalingSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    vmssForDownsize.stream().filter(vmss -> vmss.capacity() > 0).forEach(vmss -> {
      String virtualMachineScaleSetName = vmss.name();
      executionLogCallback.saveExecutionLog(
          format("Set VMSS : [%s] desired capacity to [%s]", virtualMachineScaleSetName, 0), INFO);
      updateVMSSCapacity(azureConfig, setupTaskParameters, virtualMachineScaleSetName, subscriptionId,
          resourceGroupName, 0, autoScalingSteadyStateTimeout, DOWN_SCALE_COMMAND_UNIT,
          DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
    });
  }

  public void deleteVMSSs(
      AzureConfig azureConfig, List<VirtualMachineScaleSet> vmssForDelete, ExecutionLogCallback executionLogCallback) {
    List<String> vmssIds = vmssForDelete.stream().map(VirtualMachineScaleSet::id).collect(toList());

    StringBuilder virtualMachineScaleSetNames = new StringBuilder();
    vmssForDelete.forEach(vmss -> virtualMachineScaleSetNames.append(vmss.name()).append(","));

    executionLogCallback.saveExecutionLog(
        color("# Deleting Existing Virtual Machine Scale Sets: " + virtualMachineScaleSetNames, Yellow, Bold));
    azureVMSSHelperServiceDelegate.bulkDeleteVirtualMachineScaleSets(azureConfig, vmssIds);
    executionLogCallback.saveExecutionLog("Successfully deleted Virtual Scale Sets", INFO);
  }

  @NotNull
  private Integer getNewHarnessRevision(List<VirtualMachineScaleSet> harnessManagedVMSS) {
    int latestMaxRevision = getHarnessManagedScaleSetsLatestRevision(harnessManagedVMSS);
    return latestMaxRevision + 1;
  }

  private int getHarnessManagedScaleSetsLatestRevision(List<VirtualMachineScaleSet> harnessManagedScaleSets) {
    return harnessManagedScaleSets.stream().mapToInt(this ::getScaleSetLatestRevision).max().orElse(0);
  }

  private int getScaleSetLatestRevision(VirtualMachineScaleSet scaleSet) {
    return scaleSet.tags()
        .entrySet()
        .stream()
        .filter(tagEntry -> tagEntry.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG_NAME))
        .mapToInt(tagEntry -> getRevisionFromTag(tagEntry.getValue()))
        .max()
        .orElse(0);
  }

  @NotNull
  private String getNewVirtualMachineScaleSetName(
      AzureVMSSSetupTaskParameters setupTaskParameters, Integer harnessRevision) {
    String vmssNamePrefix = setupTaskParameters.getVmssNamePrefix();
    if (isBlank(vmssNamePrefix)) {
      throw new InvalidRequestException("Virtual Machine Scale Set prefix name can't be null or empty");
    }
    return getAsgName(vmssNamePrefix, harnessRevision);
  }

  private void createVirtualMachineScaleSet(AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters,
      Integer newHarnessRevision, String newVirtualMachineScaleSetName, ExecutionLogCallback logCallback) {
    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();
    String baseVirtualMachineScaleSetName = setupTaskParameters.getBaseVMSSName();
    String infraMappingId = setupTaskParameters.getInfraMappingId();
    boolean isBlueGreen = setupTaskParameters.isBlueGreen();

    AzureUserAuthVMInstanceData azureUserAuthVMInstanceData = buildUserAuthVMInstanceData(setupTaskParameters);

    // Get base VMSS based on provided scale set name, subscriptionId, resourceGroupName provided by task parameters
    logCallback.saveExecutionLog("Getting base Virtual Machine Scale Set", INFO);
    VirtualMachineScaleSet baseVirtualMachineScaleSet = getBaseVirtualMachineScaleSet(
        azureConfig, subscriptionId, resourceGroupName, baseVirtualMachineScaleSetName, logCallback);

    // Create new VMSS based on Base VMSS configuration
    logCallback.saveExecutionLog(
        format("Creating new Virtual Machine Scale Set [%s]", newVirtualMachineScaleSetName), INFO);
    azureVMSSHelperServiceDelegate.createVirtualMachineScaleSet(azureConfig, baseVirtualMachineScaleSet, infraMappingId,
        newVirtualMachineScaleSetName, newHarnessRevision, azureUserAuthVMInstanceData, isBlueGreen);
  }

  private AzureUserAuthVMInstanceData buildUserAuthVMInstanceData(AzureVMSSSetupTaskParameters setupTaskParameters) {
    String vmssAuthType = setupTaskParameters.getVmssAuthType();
    String username = setupTaskParameters.getUserName();
    String password =
        vmssAuthType.equals(VMSS_AUTH_TYPE_DEFAULT) ? setupTaskParameters.getPassword() : StringUtils.EMPTY;
    String sshPublicKey =
        vmssAuthType.equals(VMSS_AUTH_TYPE_SSH_PUBLIC_KEY) ? setupTaskParameters.getSshPublicKey() : StringUtils.EMPTY;
    return AzureUserAuthVMInstanceData.builder()
        .vmssAuthType(vmssAuthType)
        .userName(username)
        .password(password)
        .sshPublicKey(sshPublicKey)
        .build();
  }

  private VirtualMachineScaleSet getBaseVirtualMachineScaleSet(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String baseVirtualMachineScaleSetName, ExecutionLogCallback logCallback) {
    Optional<VirtualMachineScaleSet> baseVirtualMachineScaleSetOp =
        azureVMSSHelperServiceDelegate.getVirtualMachineScaleSetByName(
            azureConfig, subscriptionId, resourceGroupName, baseVirtualMachineScaleSetName);

    if (!baseVirtualMachineScaleSetOp.isPresent()) {
      String errorMessage = format(
          "Couldn't find reference baseVirtualMachineScaleSetName: [%s] in resourceGroupName: [%s] and subscriptionId: [%s]",
          baseVirtualMachineScaleSetName, resourceGroupName, subscriptionId);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      logger.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
    return baseVirtualMachineScaleSetOp.get();
  }

  private AzureVMSSSetupTaskResponse buildAzureVMSSSetupTaskResponse(AzureConfig azureConfig, Integer harnessRevision,
      String newVirtualMachineScaleSetName, VirtualMachineScaleSet mostRecentActiveVMSS,
      AzureVMSSSetupTaskParameters setupTaskParameters, ExecutionLogCallback logCallback) {
    boolean isBlueGreen = setupTaskParameters.isBlueGreen();
    boolean isUseCurrentRunningCount = setupTaskParameters.isUseCurrentRunningCount();
    String baseVirtualMachineScaleSetName = setupTaskParameters.getBaseVMSSName();
    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();

    AzureVMSSAutoScalingData azureVMSSAutoScalingData = getAzureVMSSAutoScalingData(
        azureConfig, mostRecentActiveVMSS, setupTaskParameters, logCallback, isUseCurrentRunningCount);

    String mostRecentActiveVMSSName = mostRecentActiveVMSS == null ? StringUtils.EMPTY : mostRecentActiveVMSS.name();

    List<String> baseVMSSScalingPolicyJSONs =
        getScalingPolicyJSONs(azureConfig, subscriptionId, resourceGroupName, baseVirtualMachineScaleSetName);
    AzureVMSSPreDeploymentData azureVMSSPreDeploymentData =
        populatePreDeploymentData(azureConfig, subscriptionId, mostRecentActiveVMSS);

    return AzureVMSSSetupTaskResponse.builder()
        .lastDeployedVMSSName(mostRecentActiveVMSSName)
        .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
        .harnessRevision(harnessRevision)
        .minInstances(azureVMSSAutoScalingData.getMinInstances())
        .maxInstances(azureVMSSAutoScalingData.getMaxInstances())
        .desiredInstances(azureVMSSAutoScalingData.getDesiredInstances())
        .blueGreen(isBlueGreen)
        .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
        .baseVMSSScalingPolicyJSONs(baseVMSSScalingPolicyJSONs)
        .preDeploymentData(azureVMSSPreDeploymentData)
        .build();
  }

  private AzureVMSSAutoScalingData getAzureVMSSAutoScalingData(AzureConfig azureConfig,
      VirtualMachineScaleSet mostRecentActiveVMSS, AzureVMSSSetupTaskParameters setupTaskParameters,
      ExecutionLogCallback logCallback, boolean isUseCurrentRunningCount) {
    int minInstances = DEFAULT_AZURE_VMSS_MIN_INSTANCES;
    int maxInstances = DEFAULT_AZURE_VMSS_MAX_INSTANCES;
    int desiredInstances = DEFAULT_AZURE_VMSS_DESIRED_INSTANCES;

    if (isUseCurrentRunningCount) {
      String mostRecentActiveVMSSName = StringUtils.EMPTY;
      if (mostRecentActiveVMSS != null) {
        // Get scaling policy for mostRecentActiveVMSS
        Optional<AutoscaleProfile> defaultAutoScaleProfileOp =
            azureAutoScaleSettingsHelperServiceDelegate.getDefaultAutoScaleProfile(
                azureConfig, mostRecentActiveVMSS.resourceGroupName(), mostRecentActiveVMSS.id());
        if (defaultAutoScaleProfileOp.isPresent()) {
          AutoscaleProfile defaultAutoScaleProfile = defaultAutoScaleProfileOp.get();
          int defaultAPMinInstance = defaultAutoScaleProfile.minInstanceCount();
          int defaultAPMaxInstance = defaultAutoScaleProfile.maxInstanceCount();
          minInstances = defaultAPMinInstance;
          maxInstances = defaultAPMaxInstance;
        }
        desiredInstances = mostRecentActiveVMSS.capacity();
        mostRecentActiveVMSSName = mostRecentActiveVMSS.name();
      }
      logCallback.saveExecutionLog(format("Using currently running min: [%d], max: [%d], desired: [%d] from Asg: [%s]",
                                       minInstances, maxInstances, desiredInstances, mostRecentActiveVMSSName),
          INFO);
    } else {
      minInstances = setupTaskParameters.getMinInstances();
      maxInstances = setupTaskParameters.getMaxInstances();
      desiredInstances = setupTaskParameters.getDesiredInstances();
      logCallback.saveExecutionLog(format("Using workflow input min: [%d], max: [%d] and desired: [%d]", minInstances,
                                       maxInstances, desiredInstances),
          INFO);
    }

    return AzureVMSSAutoScalingData.builder()
        .minInstances(minInstances)
        .maxInstances(maxInstances)
        .desiredInstances(desiredInstances)
        .build();
  }

  private AzureVMSSPreDeploymentData populatePreDeploymentData(
      AzureConfig azureConfig, String subscriptionId, VirtualMachineScaleSet mostRecentActiveVMSS) {
    boolean isMostRecentVMSSAvailable = mostRecentActiveVMSS != null;

    List<String> autoScalingPoliciesJson = isMostRecentVMSSAvailable
        ? getScalingPolicyJSONs(
              azureConfig, subscriptionId, mostRecentActiveVMSS.resourceGroupName(), mostRecentActiveVMSS.name())
        : emptyList();

    return AzureVMSSPreDeploymentData.builder()
        .minCapacity(0)
        .desiredCapacity(isMostRecentVMSSAvailable ? mostRecentActiveVMSS.capacity() : 0)
        .scalingPolicyJSON(isMostRecentVMSSAvailable ? autoScalingPoliciesJson : emptyList())
        .oldVmssName(isMostRecentVMSSAvailable ? mostRecentActiveVMSS.name() : StringUtils.EMPTY)
        .build();
  }

  private List<String> getScalingPolicyJSONs(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    Optional<VirtualMachineScaleSet> virtualMachineScaleSetOp =
        azureVMSSHelperServiceDelegate.getVirtualMachineScaleSetByName(
            azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);

    return virtualMachineScaleSetOp
        .flatMap(virtualMachineScaleSet
            -> azureAutoScaleSettingsHelperServiceDelegate.getAutoScaleSettingJSONByTargetResourceId(
                azureConfig, resourceGroupName, virtualMachineScaleSet.id()))
        .map(Collections::singletonList)
        .orElse(Collections.emptyList());
  }
}
