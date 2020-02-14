package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.pcf.PcfManifestFileData;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfSetupCommandTaskHandler extends PcfCommandTaskHandler {
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;

  /**
   * This method is responsible for fetching previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   */
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfCommandSetupRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfCommandSetupRequest"));
    }
    executionLogCallback.saveExecutionLog(color("---------- Starting PCF App Setup Command", White, Bold));
    PcfManifestFileData pcfManifestFileData = PcfManifestFileData.builder().varFiles(new ArrayList<>()).build();
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);
    PcfCommandSetupRequest pcfCommandSetupRequest = (PcfCommandSetupRequest) pcfCommandRequest;
    File artifactFile = null;
    File workingDirectory = null;
    try {
      // This path represents location where artifact will be downloaded, manifest file will be created and
      // config.json file will be generated with login details by cf cli, for current task.
      // This value is set to CF_HOME env variable when process executor is created.
      String randomToken = UUIDGenerator.generateUuid();
      workingDirectory = pcfCommandTaskHelper.generateWorkingDirectoryForDeployment(randomToken);
      if (pcfCommandSetupRequest.isUseCfCLI() || pcfCommandSetupRequest.isUseAppAutoscalar()) {
        if (workingDirectory == null) {
          throw new PivotalClientApiException("Failed to generate CF-CLI Working directory");
        }
      }

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .orgName(pcfCommandSetupRequest.getOrganization())
                                              .spaceName(pcfCommandSetupRequest.getSpace())
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .timeOutIntervalInMins(pcfCommandSetupRequest.getTimeoutIntervalInMin())
                                              .useCFCLI(pcfCommandSetupRequest.isUseCfCLI())
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
      if (EmptyPredicate.isEmpty(previousReleases)) {
        executionLogCallback.saveExecutionLog("# No Existing applications found");
      } else {
        StringBuilder appNames = new StringBuilder(color("# Existing applications: ", White, Bold));
        previousReleases.forEach(applicationSummary -> appNames.append("\n").append(applicationSummary.getName()));
        executionLogCallback.saveExecutionLog(appNames.toString());
      }

      // Get new Revision version
      // @TODO: add unit test
      int releaseRevision = getReleaseRevisionForNewApplication(previousReleases);

      // Delete any older application excpet most recent 1.
      deleteOlderApplications(previousReleases, pcfRequestConfig, pcfCommandSetupRequest, pcfAppAutoscalarRequestData,
          executionLogCallback);

      // Fetch apps again, as apps may have been deleted/downsized
      previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, pcfCommandSetupRequest.getReleaseNamePrefix());

      Integer totalPreviousInstanceCount = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.stream().mapToInt(ApplicationSummary ::getInstances).sum();

      Integer instanceCountForMostRecentVersion = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.get(previousReleases.size() - 1).getRunningInstances();

      // New appName to be created
      String newReleaseName =
          ServiceVersionConvention.getServiceName(pcfCommandSetupRequest.getReleaseNamePrefix(), releaseRevision);

      // Download artifact on delegate from manager
      artifactFile = pcfCommandTaskHelper.downloadArtifact(
          pcfCommandSetupRequest.getArtifactFiles(), pcfCommandSetupRequest.getAccountId(), workingDirectory);

      boolean varsYmlPresent = checkIfVarsFilePresent(pcfCommandSetupRequest);
      PcfCreateApplicationRequestData requestData = PcfCreateApplicationRequestData.builder()
                                                        .pcfRequestConfig(pcfRequestConfig)
                                                        .artifactPath(artifactFile.getAbsolutePath())
                                                        .configPathVar(workingDirectory.getAbsolutePath())
                                                        .setupRequest(pcfCommandSetupRequest)
                                                        .newReleaseName(newReleaseName)
                                                        .pcfManifestFileData(pcfManifestFileData)
                                                        .varsYmlFilePresent(varsYmlPresent)
                                                        .build();

      // Generate final manifest Yml needed for push.

      requestData.setFinalManifestYaml(pcfCommandTaskHelper.generateManifestYamlForPush(requestData));

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
      ApplicationDetail newApplication = pcfDeploymentManager.createApplication(requestData, executionLogCallback);
      executionLogCallback.saveExecutionLog(color("# Application created successfully", White, Bold));
      executionLogCallback.saveExecutionLog("# App Details: ");
      pcfCommandTaskHelper.printApplicationDetail(newApplication, executionLogCallback);

      List<PcfAppSetupTimeDetails> downsizeAppDetails =
          pcfCommandTaskHelper.generateDownsizeDetails(pcfRequestConfig, newReleaseName);

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
              .downsizeDetails(downsizeAppDetails)
              .build();

      executionLogCallback.saveExecutionLog(
          "\n ----------  PCF Setup process completed successfully", INFO, CommandExecutionStatus.SUCCESS);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(pcfSetupCommandResponse.getCommandExecutionStatus())
          .errorMessage(pcfSetupCommandResponse.getOutput())
          .pcfCommandResponse(pcfSetupCommandResponse)
          .build();

    } catch (RuntimeException | PivotalClientApiException | IOException | ExecutionException e) {
      logger.error(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Setup task [{}]", pcfCommandSetupRequest, e);
      executionLogCallback.saveExecutionLog(
          "\n\n ----------  PCF Setup process failed to complete successfully", ERROR, CommandExecutionStatus.FAILURE);
      Misc.logAllMessages(e, executionLogCallback);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(e))
          .build();
    } finally {
      // Delete downloaded artifact and generated manifest.yaml file
      removeTempFilesCreated((PcfCommandSetupRequest) pcfCommandRequest, executionLogCallback, artifactFile,
          workingDirectory, pcfManifestFileData);
    }
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
      filesToBeRemoved.add(pcfManifestFileData.getManifestFile());
      pcfManifestFileData.getVarFiles().forEach(filesToBeRemoved::add);

      if (artifactFile != null) {
        filesToBeRemoved.add(artifactFile);
      }

      if (pcfCommandRequest.isUseCfCLI()) {
        File manifestYamlFile = pcfManifestFileData.getManifestFile();
        filesToBeRemoved.add(
            new File(pcfCommandTaskHelper.generateFinalManifestFilePath(manifestYamlFile.getAbsolutePath())));
      }

      pcfCommandTaskHelper.deleteCreatedFile(filesToBeRemoved);

      if (workingDirectory != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      }
    } catch (Exception e) {
      logger.warn("Failed to remove temp files created", e);
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
   */
  @VisibleForTesting
  void deleteOlderApplications(List<ApplicationSummary> previousReleases, PcfRequestConfig pcfRequestConfig,
      PcfCommandSetupRequest pcfCommandSetupRequest, PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      return;
    }

    Integer olderActiveVersionCountToKeep = pcfCommandSetupRequest.getOlderActiveVersionCountToKeep() == null
        ? Integer.valueOf(MAX_RELEASE_VERSIONS_TO_KEEP)
        : pcfCommandSetupRequest.getOlderActiveVersionCountToKeep();

    Set<String> appsDeleted = new HashSet<>();

    List<ApplicationSummary> appsWithZeroInstances =
        previousReleases.stream()
            .filter(applicationSummary -> applicationSummary.getInstances().intValue() == 0)
            .collect(toList());

    if (isNotEmpty(appsWithZeroInstances)) {
      executionLogCallback.saveExecutionLog("\n# Deleting Applications with 0 instances");
      appsWithZeroInstances.forEach(applicationSummary -> {
        deleteApplication(applicationSummary, pcfRequestConfig, appsDeleted, executionLogCallback);
      });
    }

    List<ApplicationSummary> appsWithNonZeroInstances =
        previousReleases.stream()
            .filter(applicationSummary -> applicationSummary.getInstances().intValue() > 0)
            .sorted(comparingInt(
                applicationSummary -> pcfCommandTaskHelper.getRevisionFromReleaseName(applicationSummary.getName())))
            .collect(toList());

    logApplicationsRetentionsMsg(appsWithNonZeroInstances, olderActiveVersionCountToKeep, executionLogCallback);

    // At this point, all apps with 0 instances have been deleted.
    // Now, we need to keep "olderActiveVersionCountToKeep" no of apps.
    // We will keep most recent one as is, and downsize olderActiveVersionCountToKeep - 1
    // apps to 0, so they will be deleted in next deployment.
    if (isNotEmpty(appsWithNonZeroInstances) && appsWithNonZeroInstances.size() > 1) {
      int lastIndexToDelete = appsWithNonZeroInstances.size() - olderActiveVersionCountToKeep - 1;
      for (int index = 0; index < appsWithNonZeroInstances.size() - 1; index++) {
        ApplicationSummary applicationSummary = appsWithNonZeroInstances.get(index);
        if (index <= lastIndexToDelete) {
          deleteApplication(applicationSummary, pcfRequestConfig, appsDeleted, executionLogCallback);
        } else {
          downsizeApplicationToZero(applicationSummary, pcfRequestConfig, pcfCommandSetupRequest,
              appAutoscalarRequestData, executionLogCallback);
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

  private void logApplicationsRetentionsMsg(List<ApplicationSummary> appsWithNonZeroInstances,
      Integer olderActiveVersionCountToKeep, ExecutionLogCallback executionLogCallback) {
    if (isEmpty(appsWithNonZeroInstances)) {
      return;
    }

    executionLogCallback.saveExecutionLog("\n# Processing Apps with Non-Zero Instances");
    executionLogCallback.saveExecutionLog(
        new StringBuilder(128)
            .append("# No Change For Most Recent Application: ")
            .append(appsWithNonZeroInstances.get(appsWithNonZeroInstances.size() - 1).getName())
            .toString());

    if (appsWithNonZeroInstances.size() > 2) {
      for (int i = appsWithNonZeroInstances.size() - 2; i >= 0; i--) {
        String msg;
        if (i < olderActiveVersionCountToKeep) {
          msg = new StringBuilder(128)
                    .append("# Application: ")
                    .append(appsWithNonZeroInstances.get(i).getName())
                    .append(" Will Be Downsized To 0")
                    .toString();
        } else {
          msg = new StringBuilder(128)
                    .append("# Application: ")
                    .append(appsWithNonZeroInstances.get(i).getName())
                    .append(" Will Be Deleted")
                    .toString();
        }

        executionLogCallback.saveExecutionLog(msg);
      }
    }
    executionLogCallback.saveExecutionLog("");
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
