package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.beans.command.PcfDummyCommandUnit.CheckExistingApps;
import static software.wings.beans.command.PcfDummyCommandUnit.PcfSetup;
import static software.wings.beans.command.PcfDummyCommandUnit.Wrapup;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.pcf.PcfManifestFileData;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;
import io.harness.pcf.PivotalClientApiException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.PcfConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.settings.SettingValue;
import software.wings.utils.ServiceVersionConvention;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@NoArgsConstructor
@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
public class PcfSetupCommandTaskHandler extends PcfCommandTaskHandler {
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;

  /**
   * This method is responsible for fetching previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   */
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync) {
    if (!(pcfCommandRequest instanceof PcfCommandSetupRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfCommandSetupRequest"));
    }
    PcfManifestFileData pcfManifestFileData = PcfManifestFileData.builder().varFiles(new ArrayList<>()).build();
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails, false);
    PcfCommandSetupRequest pcfCommandSetupRequest = (PcfCommandSetupRequest) pcfCommandRequest;
    decryptArtifactRepositoryPassword(pcfCommandSetupRequest);
    File artifactFile = null;
    File workingDirectory = null;

    try {
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), CheckExistingApps);

      workingDirectory = generateWorkingDirectoryOnDelegate(pcfCommandSetupRequest);

      PcfRequestConfig pcfRequestConfig =
          PcfRequestConfig.builder()
              .orgName(pcfCommandSetupRequest.getOrganization())
              .spaceName(pcfCommandSetupRequest.getSpace())
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .timeOutIntervalInMins(pcfCommandSetupRequest.getTimeoutIntervalInMin())
              .useCFCLI(pcfCommandSetupRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskHelper.getCfCliPathOnDelegate(
                  pcfCommandRequest.isUseCfCLI(), pcfCommandRequest.getCfCliVersion()))
              .cfCliVersion(pcfCommandRequest.getCfCliVersion())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .limitPcfThreads(pcfCommandSetupRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(pcfCommandSetupRequest.isIgnorePcfConnectionContextCache())
              .build();

      PcfAppAutoscalarRequestData pcfAppAutoscalarRequestData =
          PcfAppAutoscalarRequestData.builder()
              .pcfRequestConfig(pcfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(pcfCommandSetupRequest.getTimeoutIntervalInMin())
              .build();

      executionLogCallback.saveExecutionLog("\n# Fetching all existing applications ");

      // Get all previous release names in desending order of version number
      List<ApplicationSummary> previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, pcfCommandSetupRequest.getReleaseNamePrefix());

      // Print Existing applications information
      printExistingApplicationsDetails(executionLogCallback, previousReleases);

      // currently Active version is stamped for BG only.
      ApplicationSummary activeApplication =
          findActiveApplication(executionLogCallback, pcfCommandSetupRequest, pcfRequestConfig, previousReleases);

      ApplicationSummary mostRecentInactiveApplication = null;
      if (pcfCommandSetupRequest.isBlueGreen()) {
        mostRecentInactiveApplication = getMostRecentInactiveApplication(
            executionLogCallback, activeApplication, pcfCommandSetupRequest, previousReleases);
      }

      // Get new Revision version
      int releaseRevision = getReleaseRevisionForNewApplication(previousReleases);

      // Delete any older application excpet most recent 1.
      deleteOlderApplications(previousReleases, pcfRequestConfig, pcfCommandSetupRequest, pcfAppAutoscalarRequestData,
          activeApplication, executionLogCallback);
      executionLogCallback.saveExecutionLog("Completed Checking Existing Application", INFO, SUCCESS);

      // Fetch apps again, as apps may have been deleted/downsized
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), PcfSetup);
      executionLogCallback.saveExecutionLog(color("---------- Starting PCF App Setup Command", White, Bold));
      previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, pcfCommandSetupRequest.getReleaseNamePrefix());

      Integer totalPreviousInstanceCount = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.stream().mapToInt(ApplicationSummary::getInstances).sum();

      Integer instanceCountForMostRecentVersion = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.get(previousReleases.size() - 1).getRunningInstances();

      // New appName to be created
      String newReleaseName =
          ServiceVersionConvention.getServiceName(pcfCommandSetupRequest.getReleaseNamePrefix(), releaseRevision);
      if (!pcfCommandSetupRequest.getArtifactStreamAttributes().isDockerBasedDeployment()) {
        artifactFile = fetchArtifactFileForDeployment(pcfCommandSetupRequest, workingDirectory, executionLogCallback);
      }

      boolean varsYmlPresent = checkIfVarsFilePresent(pcfCommandSetupRequest);
      PcfCreateApplicationRequestData requestData =
          PcfCreateApplicationRequestData.builder()
              .pcfRequestConfig(pcfRequestConfig)
              .artifactPath(artifactFile == null ? null : artifactFile.getAbsolutePath())
              .configPathVar(workingDirectory.getAbsolutePath())
              .setupRequest(pcfCommandSetupRequest)
              .newReleaseName(newReleaseName)
              .pcfManifestFileData(pcfManifestFileData)
              .varsYmlFilePresent(varsYmlPresent)
              .build();

      // Generate final manifest Yml needed for push.
      requestData.setFinalManifestYaml(
          pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData));
      // Create manifest.yaml file
      prepareManifestYamlFile(requestData);
      // create vars file if needed
      if (varsYmlPresent) {
        prepareVarsYamlFile(requestData);
      }

      // Create new Application
      executionLogCallback.saveExecutionLog(color("\n# Creating new Application", White, Bold));
      // Update pcfRequestConfig with details to create application
      updatePcfRequestConfig(pcfCommandSetupRequest, pcfRequestConfig, newReleaseName);
      // create PCF Application
      ApplicationDetail newApplication = createAppAndPrintDetails(executionLogCallback, requestData);

      List<PcfAppSetupTimeDetails> downsizeAppDetails = pcfCommandTaskHelper.generateDownsizeDetails(activeApplication);

      PcfAppSetupTimeDetails mostRecentInactiveAppVersionDetails = null;
      if (mostRecentInactiveApplication != null) {
        List<String> mostRecentInactiveAppVersionUrls = new ArrayList<>();
        mostRecentInactiveAppVersionUrls.addAll(mostRecentInactiveApplication.getUrls());
        mostRecentInactiveAppVersionDetails =
            PcfAppSetupTimeDetails.builder()
                .applicationGuid(mostRecentInactiveApplication.getId())
                .applicationName(mostRecentInactiveApplication.getName())
                .initialInstanceCount(mostRecentInactiveApplication.getRunningInstances())
                .urls(mostRecentInactiveAppVersionUrls)
                .build();
      }

      List<String> urls = new ArrayList<>();
      urls.addAll(newApplication.getUrls());

      PcfSetupCommandResponse pcfSetupCommandResponse =
          PcfSetupCommandResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .output(StringUtils.EMPTY)
              .newApplicationDetails(PcfAppSetupTimeDetails.builder()
                                         .applicationGuid(newApplication.getId())
                                         .applicationName(newApplication.getName())
                                         .urls(urls)
                                         .initialInstanceCount(0)
                                         .build())
              .totalPreviousInstanceCount(totalPreviousInstanceCount)
              .instanceCountForMostRecentVersion(instanceCountForMostRecentVersion)
              .mostRecentInactiveAppVersion(mostRecentInactiveAppVersionDetails)
              .downsizeDetails(downsizeAppDetails)
              .build();

      executionLogCallback.saveExecutionLog("\n ----------  PCF Setup process completed successfully", INFO, SUCCESS);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(pcfSetupCommandResponse.getCommandExecutionStatus())
          .errorMessage(pcfSetupCommandResponse.getOutput())
          .pcfCommandResponse(pcfSetupCommandResponse)
          .build();

    } catch (RuntimeException | PivotalClientApiException | IOException | ExecutionException e) {
      log.error(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Setup task [{}]", pcfCommandSetupRequest, e);
      executionLogCallback.saveExecutionLog(
          "\n\n ----------  PCF Setup process failed to complete successfully", ERROR, CommandExecutionStatus.FAILURE);
      Misc.logAllMessages(e, executionLogCallback);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(e))
          .build();
    } finally {
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Wrapup);
      // Delete downloaded artifact and generated manifest.yaml file
      removeTempFilesCreated((PcfCommandSetupRequest) pcfCommandRequest, executionLogCallback, artifactFile,
          workingDirectory, pcfManifestFileData);
      executionLogCallback.saveExecutionLog("#----------  Cleaning up temporary files completed", INFO, SUCCESS);
    }
  }

  private void decryptArtifactRepositoryPassword(PcfCommandSetupRequest pcfCommandSetupRequest) {
    ArtifactStreamAttributes artifactStreamAttributes = pcfCommandSetupRequest.getArtifactStreamAttributes();
    if (artifactStreamAttributes.isDockerBasedDeployment()) {
      SettingValue settingValue = artifactStreamAttributes.getServerSetting().getValue();
      List<EncryptedDataDetail> artifactServerEncryptedDataDetails =
          pcfCommandSetupRequest.getArtifactStreamAttributes().getArtifactServerEncryptedDataDetails();
      encryptionService.decrypt((EncryptableSetting) settingValue, artifactServerEncryptedDataDetails, false);
    }
  }

  private File generateWorkingDirectoryOnDelegate(PcfCommandSetupRequest pcfCommandSetupRequest)
      throws PivotalClientApiException, IOException {
    // This path represents location where artifact will be downloaded, manifest file will be created and
    // config.json file will be generated with login details by cf cli, for current task.
    // This value is set to CF_HOME env variable when process executor is created.
    File workingDirectory = pcfCommandTaskHelper.generateWorkingDirectoryForDeployment();
    if (pcfCommandSetupRequest.isUseCfCLI() || pcfCommandSetupRequest.isUseAppAutoscalar()) {
      if (workingDirectory == null) {
        throw new PivotalClientApiException("Failed to generate CF-CLI Working directory");
      }
    }

    return workingDirectory;
  }

  private File fetchArtifactFileForDeployment(PcfCommandSetupRequest pcfCommandSetupRequest, File workingDirectory,
      ExecutionLogCallback executionLogCallback) throws IOException, ExecutionException {
    File artifactFile;
    if (pcfCommandSetupRequest.getArtifactStreamAttributes().isMetadataOnly()) {
      executionLogCallback.saveExecutionLog(
          color("--------- artifact will be downloaded for only-meta feature", White));
      artifactFile =
          pcfCommandTaskHelper.downloadArtifact(pcfCommandSetupRequest, workingDirectory, executionLogCallback);
    } else {
      // Download artifact on delegate from manager
      artifactFile = pcfCommandTaskHelper.downloadArtifactFromManager(
          executionLogCallback, pcfCommandSetupRequest, workingDirectory);
    }

    return artifactFile;
  }

  private ApplicationSummary getMostRecentInactiveApplication(ExecutionLogCallback executionLogCallback,
      ApplicationSummary activeApplicationSummary, PcfCommandSetupRequest pcfCommandSetupRequest,
      List<ApplicationSummary> previousReleases) {
    if (isEmpty(previousReleases)) {
      return null;
    }

    ApplicationSummary mostRecentInactiveApplication = null;

    mostRecentInactiveApplication =
        previousReleases.stream()
            .filter(applicationSummary
                -> applicationSummary.getInstances() > 0 && !applicationSummary.equals(activeApplicationSummary))
            .reduce((first, second) -> second)
            .orElse(null);

    if (mostRecentInactiveApplication == null && previousReleases.size() > 1) {
      mostRecentInactiveApplication = previousReleases.get(previousReleases.size() - 2);
    }

    return mostRecentInactiveApplication;
  }

  private ApplicationSummary findActiveApplication(ExecutionLogCallback executionLogCallback,
      PcfCommandSetupRequest pcfCommandSetupRequest, PcfRequestConfig pcfRequestConfig,
      List<ApplicationSummary> previousReleases) throws PivotalClientApiException {
    if (isEmpty(previousReleases)) {
      return null;
    }

    ApplicationSummary currentActiveApplication = null;
    // For BG, check for Environment Variable stamped to denote active version, "HARNESS__STATUS__INDENTIFIER: ACTIVE"
    if (pcfCommandSetupRequest.isBlueGreen()) {
      currentActiveApplication =
          pcfCommandTaskHelper.findCurrentActiveApplication(previousReleases, pcfRequestConfig, executionLogCallback);
    }

    // If not found, get Most recent version with non-zero count.
    if (currentActiveApplication == null) {
      currentActiveApplication = previousReleases.stream()
                                     .filter(applicationSummary -> applicationSummary.getInstances() > 0)
                                     .reduce((first, second) -> second)
                                     .orElse(null);
    }

    // All applications have 0 instances
    if (currentActiveApplication == null) {
      currentActiveApplication = previousReleases.get(previousReleases.size() - 1);
    }

    return currentActiveApplication;
  }

  private void printExistingApplicationsDetails(
      ExecutionLogCallback executionLogCallback, List<ApplicationSummary> previousReleases) {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      executionLogCallback.saveExecutionLog("# No Existing applications found");
    } else {
      StringBuilder appNames = new StringBuilder(color("# Existing applications: ", White, Bold));
      previousReleases.forEach(applicationSummary -> appNames.append("\n").append(applicationSummary.getName()));
      executionLogCallback.saveExecutionLog(appNames.toString());
    }
  }

  @VisibleForTesting
  ApplicationDetail createAppAndPrintDetails(ExecutionLogCallback executionLogCallback,
      PcfCreateApplicationRequestData requestData) throws PivotalClientApiException {
    requestData.getPcfRequestConfig().setLoggedin(false);
    ApplicationDetail newApplication = pcfDeploymentManager.createApplication(requestData, executionLogCallback);
    executionLogCallback.saveExecutionLog(color("# Application created successfully", White, Bold));
    executionLogCallback.saveExecutionLog("# App Details: ");
    pcfCommandTaskHelper.printApplicationDetail(newApplication, executionLogCallback);
    return newApplication;
  }

  @VisibleForTesting
  boolean checkIfVarsFilePresent(PcfCommandSetupRequest setupRequest) {
    if (setupRequest.getPcfManifestsPackage() == null) {
      return false;
    }

    List<String> varFiles = setupRequest.getPcfManifestsPackage().getVariableYmls();
    if (isNotEmpty(varFiles)) {
      varFiles = varFiles.stream().filter(StringUtils::isNotBlank).collect(toList());
    }

    return isNotEmpty(varFiles);
  }

  @VisibleForTesting
  void prepareManifestYamlFile(PcfCreateApplicationRequestData requestData) throws IOException {
    File manifestYamlFile = pcfCommandTaskHelper.createManifestYamlFileLocally(requestData);
    requestData.setManifestFilePath(manifestYamlFile.getAbsolutePath());
    requestData.getPcfManifestFileData().setManifestFile(manifestYamlFile);
  }

  @VisibleForTesting
  void prepareVarsYamlFile(PcfCreateApplicationRequestData requestData) throws IOException {
    if (!requestData.isVarsYmlFilePresent()) {
      return;
    }

    PcfManifestsPackage pcfManifestsPackage = requestData.getSetupRequest().getPcfManifestsPackage();
    AtomicInteger varFileIndex = new AtomicInteger(0);
    pcfManifestsPackage.getVariableYmls().forEach(varFileYml -> {
      File varsYamlFile =
          pcfCommandTaskHelper.createManifestVarsYamlFileLocally(requestData, varFileYml, varFileIndex.get());
      if (varsYamlFile != null) {
        varFileIndex.incrementAndGet();
        requestData.getPcfManifestFileData().getVarFiles().add(varsYamlFile);
      }
    });
  }

  private void updatePcfRequestConfig(
      PcfCommandSetupRequest pcfCommandSetupRequest, PcfRequestConfig pcfRequestConfig, String newReleaseName) {
    pcfRequestConfig.setApplicationName(newReleaseName);
    pcfRequestConfig.setRouteMaps(pcfCommandSetupRequest.getRouteMaps());
    pcfRequestConfig.setServiceVariables(pcfCommandSetupRequest.getServiceVariables());
    pcfRequestConfig.setSafeDisplayServiceVariables(pcfCommandSetupRequest.getSafeDisplayServiceVariables());
  }

  private int getReleaseRevisionForNewApplication(List<ApplicationSummary> previousReleases) {
    return CollectionUtils.isEmpty(previousReleases)
        ? 0
        : pcfCommandTaskHelper.getRevisionFromReleaseName(previousReleases.get(previousReleases.size() - 1).getName())
            + 1;
  }

  private void removeTempFilesCreated(PcfCommandSetupRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, File artifactFile, File workingDirectory,
      PcfManifestFileData pcfManifestFileData) {
    try {
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      List<File> filesToBeRemoved = new ArrayList<>();

      // Delete all manifests created.
      File manifestYamlFile = pcfManifestFileData.getManifestFile();
      if (manifestYamlFile != null) {
        filesToBeRemoved.add(pcfManifestFileData.getManifestFile());
      }
      pcfManifestFileData.getVarFiles().forEach(filesToBeRemoved::add);

      if (artifactFile != null) {
        filesToBeRemoved.add(artifactFile);
      }

      if (pcfCommandRequest.isUseCfCLI() && manifestYamlFile != null) {
        filesToBeRemoved.add(
            new File(pcfCommandTaskHelper.generateFinalManifestFilePath(manifestYamlFile.getAbsolutePath())));
      }

      pcfCommandTaskHelper.deleteCreatedFile(filesToBeRemoved);

      if (workingDirectory != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      }
    } catch (Exception e) {
      log.warn("Failed to remove temp files created", e);
    }
  }

  /**
   * 1. First Delete all apps  with 0 instance count
   * 2. Now process apps with non-zero apps.
   * 3. Based on count "LastVersopAppsToKeep" provided by user, (default is 3)
   * 4. Keep most recent app as is, and (last LastVersopAppsToKeep - 1) apps will be downsized to 0
   * 5. All apps older than that will be deleted
   * @param previousReleases
   * @param pcfRequestConfig
   * @param activeApplication
   */
  @VisibleForTesting
  void deleteOlderApplications(List<ApplicationSummary> previousReleases, PcfRequestConfig pcfRequestConfig,
      PcfCommandSetupRequest pcfCommandSetupRequest, PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ApplicationSummary activeApplication, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      return;
    }

    Integer olderVersionCountToKeep = pcfCommandSetupRequest.getOlderActiveVersionCountToKeep() == null
        ? Integer.valueOf(MAX_RELEASE_VERSIONS_TO_KEEP)
        : pcfCommandSetupRequest.getOlderActiveVersionCountToKeep();

    executionLogCallback.saveExecutionLog("# Existing applications to Keep: " + olderVersionCountToKeep);
    Set<String> appsDeleted = new HashSet<>();

    // Now, we need to keep "olderVersionCountToKeep" no of apps.
    // We will keep most recent/active one as is, and downsize olderActiveVersionCountToKeep - 1
    // apps to 0, so they will be deleted in next deployment.
    int olderValidAppsFound = 1;
    if (isNotEmpty(previousReleases) && previousReleases.size() > 1) {
      for (int index = previousReleases.size() - 1; index >= 0; index--) {
        ApplicationSummary applicationSummary = previousReleases.get(index);
        if (activeApplication != null && applicationSummary.getName().equals(activeApplication.getName())) {
          continue;
        } else if (olderValidAppsFound < olderVersionCountToKeep) {
          olderValidAppsFound++;
          downsizeApplicationToZero(applicationSummary, pcfRequestConfig, pcfCommandSetupRequest,
              appAutoscalarRequestData, executionLogCallback);
        } else {
          deleteApplication(applicationSummary, pcfRequestConfig, appsDeleted, executionLogCallback);
          appsDeleted.add(applicationSummary.getName());
        }
      }
    }

    if (isNotEmpty(appsDeleted)) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("# Done Deleting older applications. ")
                                                .append("Deleted Total ")
                                                .append(appsDeleted.size())
                                                .append(" applications")
                                                .toString());
    } else {
      executionLogCallback.saveExecutionLog("# No applications were eligible for deletion\n");
    }
  }

  private void deleteApplication(ApplicationSummary applicationSummary, PcfRequestConfig pcfRequestConfig,
      Set<String> appsDeleted, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("# Application Being Deleted: ").append(applicationSummary.getName()).toString());
    pcfRequestConfig.setApplicationName(applicationSummary.getName());
    try {
      pcfDeploymentManager.deleteApplication(pcfRequestConfig);
      appsDeleted.add(applicationSummary.getName());
    } catch (PivotalClientApiException e) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("Failed while deleting application: ")
                                                .append(applicationSummary.getName())
                                                .append(", Continuing for next one")
                                                .toString(),
          LogLevel.ERROR);
    }
  }

  @VisibleForTesting
  void downsizeApplicationToZero(ApplicationSummary applicationSummary, PcfRequestConfig pcfRequestConfig,
      PcfCommandSetupRequest pcfCommandSetupRequest, PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("# Application Being Downsized To 0: ")
                                              .append(applicationSummary.getName())
                                              .toString());

    if (pcfCommandSetupRequest.isUseAppAutoscalar()) {
      appAutoscalarRequestData.setApplicationName(applicationSummary.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationSummary.getId());
      appAutoscalarRequestData.setExpectedEnabled(true);
      pcfCommandTaskHelper.disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
    }

    pcfRequestConfig.setApplicationName(applicationSummary.getName());
    pcfRequestConfig.setDesiredCount(0);
    try {
      ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(pcfRequestConfig);

      // Unmap routes from application having 0 instances
      if (isNotEmpty(applicationDetail.getUrls())) {
        pcfDeploymentManager.unmapRouteMapForApplication(
            pcfRequestConfig, applicationDetail.getUrls(), executionLogCallback);
      }

      // Remove Env Variable "HARNESS__STATUS__INDENTIFIER"
      if (pcfCommandSetupRequest.isBlueGreen()) {
        pcfDeploymentManager.unsetEnvironmentVariableForAppStatus(pcfRequestConfig, executionLogCallback);
      }
    } catch (PivotalClientApiException e) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("Failed while Downsizing application: ")
                                                .append(applicationSummary.getName())
                                                .append(", Continuing for next one")
                                                .toString(),
          LogLevel.ERROR);
    }
  }
}
