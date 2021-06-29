package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.CfCommandUnitConstants.CheckExistingApps;
import static io.harness.pcf.CfCommandUnitConstants.PcfSetup;
import static io.harness.pcf.CfCommandUnitConstants.Wrapup;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.cf.PcfCommandTaskHandler;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfSetupCommandResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfManifestFileData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.request.CfCommandSetupRequest;
import software.wings.settings.SettingValue;
import software.wings.utils.ServiceVersionConvention;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
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
  @Inject protected PcfCommandTaskHelper pcfCommandTaskHelper;
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;

  /**
   * This method is responsible for fetching previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   */
  @Override
  public CfCommandExecutionResponse executeTaskInternal(CfCommandRequest cfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ILogStreamingTaskClient logStreamingTaskClient,
      boolean isInstanceSync) {
    if (!(cfCommandRequest instanceof CfCommandSetupRequest)) {
      throw new InvalidArgumentsException(Pair.of("cfCommandRequest", "Must be instance of CfCommandSetupRequest"));
    }
    LogCallback executionLogCallback = logStreamingTaskClient.obtainLogCallback(cfCommandRequest.getCommandName());
    CfManifestFileData pcfManifestFileData = CfManifestFileData.builder().varFiles(new ArrayList<>()).build();
    CfInternalConfig pcfConfig = cfCommandRequest.getPcfConfig();
    secretDecryptionService.decrypt(pcfConfig, encryptedDataDetails, false);
    CfCommandSetupRequest cfCommandSetupRequest = (CfCommandSetupRequest) cfCommandRequest;
    decryptArtifactRepositoryPassword(cfCommandSetupRequest);
    File artifactFile = null;
    File workingDirectory = null;

    try {
      executionLogCallback = logStreamingTaskClient.obtainLogCallback(CheckExistingApps);

      workingDirectory = generateWorkingDirectoryOnDelegate(cfCommandSetupRequest);

      CfRequestConfig cfRequestConfig =
          CfRequestConfig.builder()
              .orgName(cfCommandSetupRequest.getOrganization())
              .spaceName(cfCommandSetupRequest.getSpace())
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .timeOutIntervalInMins(cfCommandSetupRequest.getTimeoutIntervalInMin())
              .useCFCLI(cfCommandSetupRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskBaseHelper.getCfCliPathOnDelegate(
                  cfCommandRequest.isUseCfCLI(), cfCommandRequest.getCfCliVersion()))
              .cfCliVersion(cfCommandRequest.getCfCliVersion())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .limitPcfThreads(cfCommandSetupRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(cfCommandSetupRequest.isIgnorePcfConnectionContextCache())
              .build();

      CfAppAutoscalarRequestData pcfAppAutoscalarRequestData =
          CfAppAutoscalarRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(cfCommandSetupRequest.getTimeoutIntervalInMin())
              .build();

      executionLogCallback.saveExecutionLog("\n# Fetching all existing applications ");

      // Get all previous release names in desending order of version number
      List<ApplicationSummary> previousReleases =
          pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfCommandSetupRequest.getReleaseNamePrefix());

      // Print Existing applications information
      printExistingApplicationsDetails(executionLogCallback, previousReleases);

      // currently Active version is stamped for BG only.
      ApplicationSummary activeApplication =
          findActiveApplication(executionLogCallback, cfCommandSetupRequest, cfRequestConfig, previousReleases);

      ApplicationSummary mostRecentInactiveApplication = null;
      if (cfCommandSetupRequest.isBlueGreen()) {
        mostRecentInactiveApplication = getMostRecentInactiveApplication(
            executionLogCallback, activeApplication, cfCommandSetupRequest, previousReleases);
      }

      // Get new Revision version
      int releaseRevision = getReleaseRevisionForNewApplication(previousReleases);

      // Delete any older application excpet most recent 1.
      deleteOlderApplications(previousReleases, cfRequestConfig, cfCommandSetupRequest, pcfAppAutoscalarRequestData,
          activeApplication, executionLogCallback);
      executionLogCallback.saveExecutionLog("Completed Checking Existing Application", INFO, SUCCESS);

      // Fetch apps again, as apps may have been deleted/downsized
      executionLogCallback = logStreamingTaskClient.obtainLogCallback(PcfSetup);
      executionLogCallback.saveExecutionLog(color("---------- Starting PCF App Setup Command", White, Bold));
      previousReleases =
          pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfCommandSetupRequest.getReleaseNamePrefix());

      Integer totalPreviousInstanceCount = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.stream().mapToInt(ApplicationSummary::getInstances).sum();

      Integer instanceCountForMostRecentVersion = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.get(previousReleases.size() - 1).getRunningInstances();

      // New appName to be created
      String newReleaseName =
          ServiceVersionConvention.getServiceName(cfCommandSetupRequest.getReleaseNamePrefix(), releaseRevision);
      if (!cfCommandSetupRequest.getArtifactStreamAttributes().isDockerBasedDeployment()) {
        artifactFile = fetchArtifactFileForDeployment(cfCommandSetupRequest, workingDirectory, executionLogCallback);
      }

      boolean varsYmlPresent = checkIfVarsFilePresent(cfCommandSetupRequest);
      CfCreateApplicationRequestData requestData =
          CfCreateApplicationRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .artifactPath(artifactFile == null ? null : artifactFile.getAbsolutePath())
              .configPathVar(workingDirectory.getAbsolutePath())
              .password(pcfCommandTaskHelper.getPassword(cfCommandSetupRequest.getArtifactStreamAttributes()))
              .newReleaseName(newReleaseName)
              .pcfManifestFileData(pcfManifestFileData)
              .varsYmlFilePresent(varsYmlPresent)
              .dockerBasedDeployment(cfCommandSetupRequest.getArtifactStreamAttributes().isDockerBasedDeployment())
              .build();

      // Generate final manifest Yml needed for push.
      requestData.setFinalManifestYaml(
          pcfCommandTaskHelper.generateManifestYamlForPush(cfCommandSetupRequest, requestData));
      // Create manifest.yaml file
      prepareManifestYamlFile(requestData);
      // create vars file if needed
      if (varsYmlPresent) {
        prepareVarsYamlFile(requestData, cfCommandSetupRequest);
      }

      // Create new Application
      executionLogCallback.saveExecutionLog(color("\n# Creating new Application", White, Bold));
      // Update pcfRequestConfig with details to create application
      updatePcfRequestConfig(cfCommandSetupRequest, cfRequestConfig, newReleaseName);
      // create PCF Application
      ApplicationDetail newApplication = createAppAndPrintDetails(executionLogCallback, requestData);

      List<CfAppSetupTimeDetails> downsizeAppDetails =
          pcfCommandTaskBaseHelper.generateDownsizeDetails(activeApplication);

      CfAppSetupTimeDetails mostRecentInactiveAppVersionDetails = null;
      if (mostRecentInactiveApplication != null) {
        List<String> mostRecentInactiveAppVersionUrls = new ArrayList<>();
        mostRecentInactiveAppVersionUrls.addAll(mostRecentInactiveApplication.getUrls());
        mostRecentInactiveAppVersionDetails =
            CfAppSetupTimeDetails.builder()
                .applicationGuid(mostRecentInactiveApplication.getId())
                .applicationName(mostRecentInactiveApplication.getName())
                .initialInstanceCount(mostRecentInactiveApplication.getRunningInstances())
                .urls(mostRecentInactiveAppVersionUrls)
                .build();
      }

      List<String> urls = new ArrayList<>();
      urls.addAll(newApplication.getUrls());

      CfSetupCommandResponse cfSetupCommandResponse =
          CfSetupCommandResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .output(StringUtils.EMPTY)
              .newApplicationDetails(CfAppSetupTimeDetails.builder()
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
      return CfCommandExecutionResponse.builder()
          .commandExecutionStatus(cfSetupCommandResponse.getCommandExecutionStatus())
          .errorMessage(cfSetupCommandResponse.getOutput())
          .pcfCommandResponse(cfSetupCommandResponse)
          .build();

    } catch (RuntimeException | PivotalClientApiException | IOException | ExecutionException e) {
      log.error(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Setup task [{}]", cfCommandSetupRequest, e);
      executionLogCallback.saveExecutionLog(
          "\n\n ----------  PCF Setup process failed to complete successfully", ERROR, CommandExecutionStatus.FAILURE);
      Misc.logAllMessages(e, executionLogCallback);
      return CfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(e))
          .build();
    } finally {
      executionLogCallback = logStreamingTaskClient.obtainLogCallback(Wrapup);
      // Delete downloaded artifact and generated manifest.yaml file
      removeTempFilesCreated((CfCommandSetupRequest) cfCommandRequest, executionLogCallback, artifactFile,
          workingDirectory, pcfManifestFileData);
      executionLogCallback.saveExecutionLog("#----------  Cleaning up temporary files completed", INFO, SUCCESS);
    }
  }

  private void decryptArtifactRepositoryPassword(CfCommandSetupRequest cfCommandSetupRequest) {
    ArtifactStreamAttributes artifactStreamAttributes = cfCommandSetupRequest.getArtifactStreamAttributes();
    if (artifactStreamAttributes.isDockerBasedDeployment()) {
      SettingValue settingValue = artifactStreamAttributes.getServerSetting().getValue();
      List<EncryptedDataDetail> artifactServerEncryptedDataDetails =
          cfCommandSetupRequest.getArtifactStreamAttributes().getArtifactServerEncryptedDataDetails();
      secretDecryptionService.decrypt((EncryptableSetting) settingValue, artifactServerEncryptedDataDetails, false);
    }
  }

  private File generateWorkingDirectoryOnDelegate(CfCommandSetupRequest cfCommandSetupRequest)
      throws PivotalClientApiException, IOException {
    // This path represents location where artifact will be downloaded, manifest file will be created and
    // config.json file will be generated with login details by cf cli, for current task.
    // This value is set to CF_HOME env variable when process executor is created.
    File workingDirectory = pcfCommandTaskBaseHelper.generateWorkingDirectoryForDeployment();
    if (cfCommandSetupRequest.isUseCfCLI() || cfCommandSetupRequest.isUseAppAutoscalar()) {
      if (workingDirectory == null) {
        throw new PivotalClientApiException("Failed to generate CF-CLI Working directory");
      }
    }

    return workingDirectory;
  }

  private File fetchArtifactFileForDeployment(CfCommandSetupRequest cfCommandSetupRequest, File workingDirectory,
      LogCallback executionLogCallback) throws IOException, ExecutionException {
    File artifactFile;
    if (cfCommandSetupRequest.getArtifactStreamAttributes().isMetadataOnly()) {
      executionLogCallback.saveExecutionLog(
          color("--------- artifact will be downloaded for only-meta feature", White));
      artifactFile =
          pcfCommandTaskHelper.downloadArtifact(cfCommandSetupRequest, workingDirectory, executionLogCallback);
    } else {
      // Download artifact on delegate from manager
      artifactFile = pcfCommandTaskHelper.downloadArtifactFromManager(
          executionLogCallback, cfCommandSetupRequest, workingDirectory);
    }

    return artifactFile;
  }

  private ApplicationSummary getMostRecentInactiveApplication(LogCallback executionLogCallback,
      ApplicationSummary activeApplicationSummary, CfCommandSetupRequest cfCommandSetupRequest,
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

  private ApplicationSummary findActiveApplication(LogCallback executionLogCallback,
      CfCommandSetupRequest cfCommandSetupRequest, CfRequestConfig cfRequestConfig,
      List<ApplicationSummary> previousReleases) throws PivotalClientApiException {
    if (isEmpty(previousReleases)) {
      return null;
    }

    ApplicationSummary currentActiveApplication = null;
    // For BG, check for Environment Variable stamped to denote active version, "HARNESS__STATUS__INDENTIFIER: ACTIVE"
    if (cfCommandSetupRequest.isBlueGreen()) {
      currentActiveApplication = pcfCommandTaskBaseHelper.findCurrentActiveApplication(
          previousReleases, cfRequestConfig, executionLogCallback);
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
      LogCallback executionLogCallback, List<ApplicationSummary> previousReleases) {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      executionLogCallback.saveExecutionLog("# No Existing applications found");
    } else {
      StringBuilder appNames = new StringBuilder(color("# Existing applications: ", White, Bold));
      previousReleases.forEach(applicationSummary -> appNames.append("\n").append(applicationSummary.getName()));
      executionLogCallback.saveExecutionLog(appNames.toString());
    }
  }

  @VisibleForTesting
  ApplicationDetail createAppAndPrintDetails(
      LogCallback executionLogCallback, CfCreateApplicationRequestData requestData) throws PivotalClientApiException {
    requestData.getCfRequestConfig().setLoggedin(false);
    ApplicationDetail newApplication = pcfDeploymentManager.createApplication(requestData, executionLogCallback);
    executionLogCallback.saveExecutionLog(color("# Application created successfully", White, Bold));
    executionLogCallback.saveExecutionLog("# App Details: ");
    pcfCommandTaskBaseHelper.printApplicationDetail(newApplication, executionLogCallback);
    return newApplication;
  }

  @VisibleForTesting
  boolean checkIfVarsFilePresent(CfCommandSetupRequest setupRequest) {
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
  void prepareManifestYamlFile(CfCreateApplicationRequestData requestData) throws IOException {
    File manifestYamlFile = pcfCommandTaskBaseHelper.createManifestYamlFileLocally(requestData);
    requestData.setManifestFilePath(manifestYamlFile.getAbsolutePath());
    requestData.getPcfManifestFileData().setManifestFile(manifestYamlFile);
  }

  @VisibleForTesting
  void prepareVarsYamlFile(CfCreateApplicationRequestData requestData, CfCommandSetupRequest setupRequest)
      throws IOException {
    if (!requestData.isVarsYmlFilePresent()) {
      return;
    }

    PcfManifestsPackage pcfManifestsPackage = setupRequest.getPcfManifestsPackage();
    AtomicInteger varFileIndex = new AtomicInteger(0);
    pcfManifestsPackage.getVariableYmls().forEach(varFileYml -> {
      File varsYamlFile =
          pcfCommandTaskBaseHelper.createManifestVarsYamlFileLocally(requestData, varFileYml, varFileIndex.get());
      if (varsYamlFile != null) {
        varFileIndex.incrementAndGet();
        requestData.getPcfManifestFileData().getVarFiles().add(varsYamlFile);
      }
    });
  }

  private void updatePcfRequestConfig(
      CfCommandSetupRequest cfCommandSetupRequest, CfRequestConfig cfRequestConfig, String newReleaseName) {
    cfRequestConfig.setApplicationName(newReleaseName);
    cfRequestConfig.setRouteMaps(cfCommandSetupRequest.getRouteMaps());
    cfRequestConfig.setServiceVariables(cfCommandSetupRequest.getServiceVariables());
    cfRequestConfig.setSafeDisplayServiceVariables(cfCommandSetupRequest.getSafeDisplayServiceVariables());
  }

  private int getReleaseRevisionForNewApplication(List<ApplicationSummary> previousReleases) {
    return CollectionUtils.isEmpty(previousReleases) ? 0
                                                     : pcfCommandTaskBaseHelper.getRevisionFromReleaseName(
                                                           previousReleases.get(previousReleases.size() - 1).getName())
            + 1;
  }

  private void removeTempFilesCreated(CfCommandSetupRequest cfCommandSetupRequest, LogCallback executionLogCallback,
      File artifactFile, File workingDirectory, CfManifestFileData pcfManifestFileData) {
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

      if (cfCommandSetupRequest.isUseCfCLI() && manifestYamlFile != null) {
        filesToBeRemoved.add(
            new File(pcfCommandTaskBaseHelper.generateFinalManifestFilePath(manifestYamlFile.getAbsolutePath())));
      }

      pcfCommandTaskBaseHelper.deleteCreatedFile(filesToBeRemoved);

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
   * @param cfRequestConfig
   * @param activeApplication
   */
  @VisibleForTesting
  void deleteOlderApplications(List<ApplicationSummary> previousReleases, CfRequestConfig cfRequestConfig,
      CfCommandSetupRequest cfCommandSetupRequest, CfAppAutoscalarRequestData appAutoscalarRequestData,
      ApplicationSummary activeApplication, LogCallback executionLogCallback) throws PivotalClientApiException {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      return;
    }

    Integer olderVersionCountToKeep = cfCommandSetupRequest.getOlderActiveVersionCountToKeep() == null
        ? Integer.valueOf(MAX_RELEASE_VERSIONS_TO_KEEP)
        : cfCommandSetupRequest.getOlderActiveVersionCountToKeep();

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
          downsizeApplicationToZero(applicationSummary, cfRequestConfig, cfCommandSetupRequest,
              appAutoscalarRequestData, executionLogCallback);
        } else {
          deleteApplication(applicationSummary, cfRequestConfig, appsDeleted, executionLogCallback);
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

  private void deleteApplication(ApplicationSummary applicationSummary, CfRequestConfig cfRequestConfig,
      Set<String> appsDeleted, LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("# Application Being Deleted: ").append(applicationSummary.getName()).toString());
    cfRequestConfig.setApplicationName(applicationSummary.getName());
    try {
      pcfDeploymentManager.deleteApplication(cfRequestConfig);
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
  void downsizeApplicationToZero(ApplicationSummary applicationSummary, CfRequestConfig cfRequestConfig,
      CfCommandSetupRequest cfCommandSetupRequest, CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("# Application Being Downsized To 0: ")
                                              .append(applicationSummary.getName())
                                              .toString());

    if (cfCommandSetupRequest.isUseAppAutoscalar()) {
      appAutoscalarRequestData.setApplicationName(applicationSummary.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationSummary.getId());
      appAutoscalarRequestData.setExpectedEnabled(true);
      pcfCommandTaskBaseHelper.disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
    }

    cfRequestConfig.setApplicationName(applicationSummary.getName());
    cfRequestConfig.setDesiredCount(0);
    try {
      ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(cfRequestConfig);

      // Unmap routes from application having 0 instances
      if (isNotEmpty(applicationDetail.getUrls())) {
        pcfDeploymentManager.unmapRouteMapForApplication(
            cfRequestConfig, applicationDetail.getUrls(), executionLogCallback);
      }

      // Remove Env Variable "HARNESS__STATUS__INDENTIFIER"
      if (cfCommandSetupRequest.isBlueGreen()) {
        pcfDeploymentManager.unsetEnvironmentVariableForAppStatus(cfRequestConfig, executionLogCallback);
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
