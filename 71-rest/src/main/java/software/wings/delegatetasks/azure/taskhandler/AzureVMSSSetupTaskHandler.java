package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.GALLERY_IMAGE_ID_PATTERN;
import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.NUMBER_OF_LATEST_VERSIONS_TO_KEEP;
import static io.harness.azure.model.AzureConstants.SETUP_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_DEFAULT;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_SSH_PUBLIC_KEY;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.azure.utility.AzureResourceUtility.getRevisionFromTag;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.GalleryImage;
import com.microsoft.azure.management.compute.GalleryImageIdentifier;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureMachineImageArtifact;
import io.harness.azure.model.AzureMachineImageArtifact.ImageType;
import io.harness.azure.model.AzureMachineImageArtifact.MachineImageReference;
import io.harness.azure.model.AzureMachineImageArtifact.MachineImageReference.OsState;
import io.harness.azure.model.AzureMachineImageArtifact.OSType;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.azure.model.AzureVMSSAutoScaleSettingsData;
import io.harness.azure.model.AzureVMSSTagsData;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.GalleryImageDefinitionDTO;
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
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSSetupTaskHandler extends AzureVMSSTaskHandler {
  @Inject private AzureAutoScaleHelper azureAutoScaleHelper;

  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig) {
    AzureVMSSSetupTaskParameters setupTaskParameters = (AzureVMSSSetupTaskParameters) azureVMSSTaskParameters;
    ExecutionLogCallback logCallback = getLogCallBack(azureVMSSTaskParameters, SETUP_COMMAND_UNIT);

    logCallback.saveExecutionLog("Starting Azure Virtual Machine Scale Set Setup", INFO);

    logCallback.saveExecutionLog("Getting all Harness managed Virtual Machine Scale Sets", INFO);
    List<VirtualMachineScaleSet> harnessVMSSSortedByCreationTimeReverse =
        getHarnessMangedVMSSSortedByCreationTime(azureConfig, setupTaskParameters);
    logCallback.saveExecutionLog(
        format("Found [%s] Harness manged Virtual Machine Scale Sets", harnessVMSSSortedByCreationTimeReverse.size()),
        INFO);

    logCallback.saveExecutionLog(
        "Getting the most recent active Virtual Machine Scale Set with non zero capacity", INFO);
    VirtualMachineScaleSet mostRecentActiveVMSS = getMostRecentActiveVMSS(harnessVMSSSortedByCreationTimeReverse);
    logCallback.saveExecutionLog(mostRecentActiveVMSS != null
            ? format("Found most recent active Virtual Machine Scale Set: [%s]", mostRecentActiveVMSS.name())
            : "Couldn't find most recent active Virtual Machine Scale Set with non zero capacity",
        INFO);

    downsizeLatestVersionsKeptVMSSs(
        azureConfig, setupTaskParameters, harnessVMSSSortedByCreationTimeReverse, mostRecentActiveVMSS);
    deleteVMSSsOlderThenLatestVersionsToKeep(
        azureConfig, harnessVMSSSortedByCreationTimeReverse, mostRecentActiveVMSS, logCallback);

    logCallback.saveExecutionLog("Getting the new revision of Virtual Machine Scale Set", INFO);
    Integer newHarnessRevision = getNewHarnessRevision(harnessVMSSSortedByCreationTimeReverse);
    logCallback.saveExecutionLog(format("New revision of Virtual Machine Scale Set: [%s]", newHarnessRevision), INFO);

    logCallback.saveExecutionLog("Getting the new name of Virtual Machine Scale Set", INFO);
    String newVirtualMachineScaleSetName = getNewVirtualMachineScaleSetName(setupTaskParameters, newHarnessRevision);
    logCallback.saveExecutionLog(
        format("New name of Virtual Machine Scale Set: [%s]", newVirtualMachineScaleSetName), INFO);

    createVirtualMachineScaleSet(
        azureConfig, setupTaskParameters, newHarnessRevision, newVirtualMachineScaleSetName, logCallback);

    AzureVMSSSetupTaskResponse azureVMSSSetupTaskResponse = buildAzureVMSSSetupTaskResponse(
        azureConfig, newHarnessRevision, newVirtualMachineScaleSetName, mostRecentActiveVMSS, setupTaskParameters);

    logCallback.saveExecutionLog(
        format("Completed Azure VMSS Setup with new scale set name: [%s]", newVirtualMachineScaleSetName), INFO,
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
        azureComputeClient.listVirtualMachineScaleSetsByResourceGroupName(
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
      Date createdTimeVMss1 =
          AzureResourceUtility.iso8601BasicStrToDate(vmss1.tags().get(VMSS_CREATED_TIME_STAMP_TAG_NAME));
      Date createdTimeVMss2 =
          AzureResourceUtility.iso8601BasicStrToDate(vmss2.tags().get(VMSS_CREATED_TIME_STAMP_TAG_NAME));
      return createdTimeVMss2.compareTo(createdTimeVMss1);
    };
    return harnessManagedVMSS.stream().sorted(createdTimeComparator).collect(Collectors.toList());
  }

  private VirtualMachineScaleSet getMostRecentActiveVMSS(List<VirtualMachineScaleSet> harnessManagedScaleSets) {
    return harnessManagedScaleSets.stream()
        .filter(Objects::nonNull)
        .filter(scaleSet -> scaleSet.capacity() > 0)
        .findFirst()
        .orElse(null);
  }

  private void downsizeLatestVersionsKeptVMSSs(AzureConfig azureConfig,
      AzureVMSSSetupTaskParameters setupTaskParameters, List<VirtualMachineScaleSet> sortedHarnessManagedVMSSs,
      VirtualMachineScaleSet mostRecentActiveVMSS) {
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

    downsizeVMSSs(azureConfig, setupTaskParameters, listOfVMSSsForDownsize, subscriptionId, resourceGroupName,
        autoScalingSteadyStateVMSSTimeout);
  }

  public void downsizeVMSSs(AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters,
      List<VirtualMachineScaleSet> vmssForDownsize, String subscriptionId, String resourceGroupName,
      int autoScalingSteadyStateTimeout) {
    vmssForDownsize.stream().filter(vmss -> vmss.capacity() > 0).forEach(vmss -> {
      String virtualMachineScaleSetName = vmss.name();
      updateVMSSCapacityAndWaitForSteadyState(azureConfig, setupTaskParameters, virtualMachineScaleSetName,
          subscriptionId, resourceGroupName, 0, autoScalingSteadyStateTimeout, DOWN_SCALE_COMMAND_UNIT,
          DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
    });
  }

  private void deleteVMSSsOlderThenLatestVersionsToKeep(AzureConfig azureConfig,
      List<VirtualMachineScaleSet> sortedHarnessManagedVMSSs, VirtualMachineScaleSet mostRecentActiveVMSS,
      ExecutionLogCallback executionLogCallback) {
    if (isEmpty(sortedHarnessManagedVMSSs) || mostRecentActiveVMSS == null) {
      return;
    }

    String mostRecentActiveVMSSName = mostRecentActiveVMSS.name();
    int versionsToKeep = NUMBER_OF_LATEST_VERSIONS_TO_KEEP - 1;

    List<VirtualMachineScaleSet> listOfVMSSsForDelete =
        sortedHarnessManagedVMSSs.stream()
            .filter(Objects::nonNull)
            .filter(vmss -> !vmss.name().equals(mostRecentActiveVMSSName))
            .skip(versionsToKeep)
            .collect(toList());
    deleteVMSSs(azureConfig, listOfVMSSsForDelete, executionLogCallback);
  }

  public void deleteVMSSs(AzureConfig azureConfig, List<VirtualMachineScaleSet> scaleSetsForDelete,
      ExecutionLogCallback executionLogCallback) {
    if (scaleSetsForDelete.isEmpty()) {
      return;
    }

    List<String> vmssIds = scaleSetsForDelete.stream().map(VirtualMachineScaleSet::id).collect(toList());

    StringJoiner virtualMachineScaleSetNamesJoiner = new StringJoiner(",", "[", "]");
    scaleSetsForDelete.forEach(vmss -> virtualMachineScaleSetNamesJoiner.add(vmss.name()));

    executionLogCallback.saveExecutionLog(
        color("# Deleting existing Virtual Machine Scale Sets: " + virtualMachineScaleSetNamesJoiner.toString(), Yellow,
            Bold));
    azureComputeClient.bulkDeleteVirtualMachineScaleSets(azureConfig, vmssIds);
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
    return AzureResourceUtility.getVMSSName(vmssNamePrefix, harnessRevision);
  }

  private void createVirtualMachineScaleSet(AzureConfig azureConfig, AzureVMSSSetupTaskParameters setupTaskParameters,
      Integer newHarnessRevision, String newVirtualMachineScaleSetName, ExecutionLogCallback logCallback) {
    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();
    String baseVirtualMachineScaleSetName = setupTaskParameters.getBaseVMSSName();
    AzureMachineImageArtifactDTO imageArtifactDTO = setupTaskParameters.getImageArtifactDTO();

    AzureMachineImageArtifact imageArtifact =
        getAzureMachineImageArtifact(azureConfig, subscriptionId, resourceGroupName, imageArtifactDTO, logCallback);
    AzureVMSSTagsData azureVMSSTagsData = getAzureVMSSTagsData(setupTaskParameters, newHarnessRevision);
    AzureUserAuthVMInstanceData azureUserAuthVMInstanceData = buildUserAuthVMInstanceData(setupTaskParameters);

    // Get base VMSS based on provided scale set name, subscriptionId, resourceGroupName provided by task parameters
    logCallback.saveExecutionLog("Getting base Virtual Machine Scale Set", INFO);
    VirtualMachineScaleSet baseVirtualMachineScaleSet = getBaseVirtualMachineScaleSet(
        azureConfig, subscriptionId, resourceGroupName, baseVirtualMachineScaleSetName, logCallback);
    logCallback.saveExecutionLog(
        format("Using base Virtual Machine Scale Set: [%s]", baseVirtualMachineScaleSet.name()), INFO);

    // Create new VMSS based on Base VMSS configuration
    logCallback.saveExecutionLog(
        format("Creating new Virtual Machine Scale Set: [%s]", newVirtualMachineScaleSetName), INFO);
    azureComputeClient.createVirtualMachineScaleSet(azureConfig, baseVirtualMachineScaleSet,
        newVirtualMachineScaleSetName, azureUserAuthVMInstanceData, imageArtifact, azureVMSSTagsData);
  }

  @VisibleForTesting
  AzureMachineImageArtifact getAzureMachineImageArtifact(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, AzureMachineImageArtifactDTO azureMachineImageArtifactDTO,
      ExecutionLogCallback logCallback) {
    GalleryImageDefinitionDTO imageDefinition = azureMachineImageArtifactDTO.getImageDefinition();
    String imageDefinitionName = imageDefinition.getDefinitionName();
    String imageGalleryName = imageDefinition.getGalleryName();
    String imageVersion = imageDefinition.getVersion();

    String galleryImageId = String.format(GALLERY_IMAGE_ID_PATTERN, subscriptionId, resourceGroupName, imageGalleryName,
        imageDefinitionName, imageVersion);

    logCallback.saveExecutionLog(format("Start getting gallery image references id [%s]", galleryImageId), INFO);
    Optional<GalleryImage> galleryImageOp = azureComputeClient.getGalleryImage(
        azureConfig, subscriptionId, resourceGroupName, imageGalleryName, imageDefinitionName);
    GalleryImage galleryImage = galleryImageOp.orElseThrow(
        ()
            -> new InvalidRequestException(format(
                "Image reference cannot be found, galleryImageId: %s, imageDefinitionName: %s, subscriptionId: %s, resourceGroupName: %s, imageVersion: %s",
                imageGalleryName, imageDefinitionName, subscriptionId, resourceGroupName, imageVersion)));

    String osState = galleryImage.osState().toString();
    GalleryImageIdentifier identifier = galleryImage.identifier();
    String publisher = identifier.publisher();
    String offer = identifier.offer();
    String sku = identifier.sku();

    logCallback.saveExecutionLog(
        format("Using gallery image id [%s], publisher [%s], offer [%s],sku [%s], osState [%s]", galleryImageId,
            publisher, offer, sku, osState),
        INFO);
    return AzureMachineImageArtifact.builder()
        .imageType(ImageType.valueOf(azureMachineImageArtifactDTO.getImageType().name()))
        .osType(OSType.valueOf(azureMachineImageArtifactDTO.getImageOSType().name()))
        .imageReference(MachineImageReference.builder()
                            .id(galleryImageId)
                            .publisher(publisher)
                            .offer(offer)
                            .sku(sku)
                            .osState(OsState.fromString(osState))
                            .version(imageVersion)
                            .build())
        .build();
  }

  private AzureVMSSTagsData getAzureVMSSTagsData(
      AzureVMSSSetupTaskParameters setupTaskParameters, Integer newHarnessRevision) {
    String infraMappingId = setupTaskParameters.getInfraMappingId();
    boolean isBlueGreen = setupTaskParameters.isBlueGreen();
    return AzureVMSSTagsData.builder()
        .harnessRevision(newHarnessRevision)
        .infraMappingId(infraMappingId)
        .isBlueGreen(isBlueGreen)
        .build();
  }

  private AzureUserAuthVMInstanceData buildUserAuthVMInstanceData(AzureVMSSSetupTaskParameters setupTaskParameters) {
    AzureVMAuthDTO azureVmAuthDTO = setupTaskParameters.getAzureVmAuthDTO();
    String vmssAuthType = azureVmAuthDTO.getAzureVmAuthType().name();
    String username = azureVmAuthDTO.getUserName();
    String decryptedValue = new String(azureVmAuthDTO.getSecretRef().getDecryptedValue());
    String password = vmssAuthType.equals(VMSS_AUTH_TYPE_DEFAULT) ? decryptedValue : StringUtils.EMPTY;
    String sshPublicKey = vmssAuthType.equals(VMSS_AUTH_TYPE_SSH_PUBLIC_KEY) ? decryptedValue : StringUtils.EMPTY;

    return AzureUserAuthVMInstanceData.builder()
        .vmssAuthType(vmssAuthType)
        .userName(username)
        .password(password)
        .sshPublicKey(sshPublicKey)
        .build();
  }

  private VirtualMachineScaleSet getBaseVirtualMachineScaleSet(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String baseVirtualMachineScaleSetName, ExecutionLogCallback logCallback) {
    Optional<VirtualMachineScaleSet> baseVirtualMachineScaleSetOp = azureComputeClient.getVirtualMachineScaleSetByName(
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
      AzureVMSSSetupTaskParameters setupTaskParameters) {
    boolean isBlueGreen = setupTaskParameters.isBlueGreen();
    boolean isUseCurrentRunningCount = setupTaskParameters.isUseCurrentRunningCount();
    String baseVirtualMachineScaleSetName = setupTaskParameters.getBaseVMSSName();
    String subscriptionId = setupTaskParameters.getSubscriptionId();
    String resourceGroupName = setupTaskParameters.getResourceGroupName();

    AzureVMSSAutoScaleSettingsData azureVMSSInstanceLimits = azureAutoScaleHelper.getVMSSAutoScaleInstanceLimits(
        azureConfig, setupTaskParameters, mostRecentActiveVMSS, isUseCurrentRunningCount, SETUP_COMMAND_UNIT);

    String mostRecentActiveVMSSName = mostRecentActiveVMSS == null ? StringUtils.EMPTY : mostRecentActiveVMSS.name();

    List<String> baseVMSSScalingPolicyJSONs = azureAutoScaleHelper.getVMSSAutoScaleSettingsJSONs(
        azureConfig, subscriptionId, resourceGroupName, baseVirtualMachineScaleSetName);
    AzureVMSSPreDeploymentData azureVMSSPreDeploymentData =
        populatePreDeploymentData(azureConfig, mostRecentActiveVMSS);

    return AzureVMSSSetupTaskResponse.builder()
        .lastDeployedVMSSName(mostRecentActiveVMSSName)
        .harnessRevision(harnessRevision)
        .minInstances(azureVMSSInstanceLimits.getMinInstances())
        .maxInstances(azureVMSSInstanceLimits.getMaxInstances())
        .desiredInstances(azureVMSSInstanceLimits.getDesiredInstances())
        .blueGreen(isBlueGreen)
        .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
        .baseVMSSScalingPolicyJSONs(baseVMSSScalingPolicyJSONs)
        .preDeploymentData(azureVMSSPreDeploymentData)
        .build();
  }

  private AzureVMSSPreDeploymentData populatePreDeploymentData(
      AzureConfig azureConfig, VirtualMachineScaleSet mostRecentActiveVMSS) {
    boolean isMostRecentVMSSAvailable = mostRecentActiveVMSS != null;

    List<String> autoScalingPoliciesJson =
        azureAutoScaleHelper.getVMSSAutoScaleSettingsJSONs(azureConfig, mostRecentActiveVMSS);

    return AzureVMSSPreDeploymentData.builder()
        .minCapacity(0)
        .desiredCapacity(isMostRecentVMSSAvailable ? mostRecentActiveVMSS.capacity() : 0)
        .scalingPolicyJSON(isMostRecentVMSSAvailable ? autoScalingPoliciesJson : emptyList())
        .oldVmssName(isMostRecentVMSSAvailable ? mostRecentActiveVMSS.name() : StringUtils.EMPTY)
        .build();
  }
}
