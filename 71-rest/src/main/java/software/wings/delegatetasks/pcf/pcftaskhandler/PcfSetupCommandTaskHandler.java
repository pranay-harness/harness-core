package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor
@Singleton
public class PcfSetupCommandTaskHandler extends PcfCommandTaskHandler {
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;
  private static final Logger logger = LoggerFactory.getLogger(PcfSetupCommandTaskHandler.class);

  /**
   * This method is responsible for fetching previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   */
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    if (!(pcfCommandRequest instanceof PcfCommandSetupRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfCommandSetupRequest"));
    }
    executionLogCallback.saveExecutionLog("---------- Starting PCF App Setup Command");

    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);
    PcfCommandSetupRequest pcfCommandSetupRequest = (PcfCommandSetupRequest) pcfCommandRequest;
    try {
      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .orgName(pcfCommandSetupRequest.getOrganization())
                                              .spaceName(pcfCommandSetupRequest.getSpace())
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .timeOutIntervalInMins(pcfCommandSetupRequest.getTimeoutIntervalInMin())
                                              .build();

      executionLogCallback.saveExecutionLog("\n# Fetching all existing applications ");

      // Get all previous release names in desending order of version number
      List<ApplicationSummary> previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, pcfCommandSetupRequest.getReleaseNamePrefix());
      if (EmptyPredicate.isEmpty(previousReleases)) {
        executionLogCallback.saveExecutionLog("# No Existing applications found");
      } else {
        StringBuilder appNames = new StringBuilder("# Existing applications: ");
        previousReleases.forEach(applicationSummary -> appNames.append("\n").append(applicationSummary.getName()));
        executionLogCallback.saveExecutionLog(appNames.toString());
      }

      // Get new Revision version
      int releaseRevision = CollectionUtils.isEmpty(previousReleases)
          ? 0
          : pcfCommandTaskHelper.getRevisionFromReleaseName(previousReleases.get(previousReleases.size() - 1).getName())
              + 1;

      // Delete any older application excpet most recent 1.
      deleteOlderApplications(previousReleases, pcfRequestConfig, pcfCommandSetupRequest);

      // Fetch apps again, as apps may have been deleted/downsized
      previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, pcfCommandSetupRequest.getReleaseNamePrefix());

      Integer totalPreviousInstanceCount = CollectionUtils.isEmpty(previousReleases)
          ? Integer.valueOf(0)
          : previousReleases.stream().mapToInt(ApplicationSummary ::getInstances).sum();

      // New appName to be created
      String newReleaseName =
          ServiceVersionConvention.getServiceName(pcfCommandSetupRequest.getReleaseNamePrefix(), releaseRevision);

      // Download artifact on delegate from manager
      File artifactFile = pcfCommandTaskHelper.downloadArtifact(
          pcfCommandSetupRequest.getArtifactFiles(), pcfCommandSetupRequest.getAccountId());

      // Create manifest.yaml file
      File manifestYamlFile = pcfCommandTaskHelper.createManifestYamlFileLocally(
          pcfCommandSetupRequest, artifactFile.getAbsolutePath(), newReleaseName);

      // Create new Application
      executionLogCallback.saveExecutionLog("\n# Creating new Application");
      pcfRequestConfig.setApplicationName(newReleaseName);
      pcfRequestConfig.setRouteMaps(pcfCommandSetupRequest.getRouteMaps());
      pcfRequestConfig.setServiceVariables(pcfCommandSetupRequest.getServiceVariables());
      pcfRequestConfig.setSafeDisplayServiceVariables(pcfCommandSetupRequest.getSafeDisplayServiceVariables());

      ApplicationDetail newApplication =
          pcfDeploymentManager.createApplication(pcfRequestConfig, manifestYamlFile.getAbsolutePath());

      executionLogCallback.saveExecutionLog("# Application created successfully");
      executionLogCallback.saveExecutionLog("# App Details: ");
      pcfCommandTaskHelper.printApplicationDetail(newApplication, executionLogCallback);

      List<PcfAppSetupTimeDetails> downsizeAppDetails = pcfCommandTaskHelper.generateDownsizeDetails(
          pcfRequestConfig, newReleaseName, pcfCommandSetupRequest.getMaxCount());
      PcfSetupCommandResponse pcfSetupCommandResponse =
          PcfSetupCommandResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .output(StringUtils.EMPTY)
              .newApplicationDetails(PcfAppSetupTimeDetails.builder()
                                         .applicationGuid(newApplication.getId())
                                         .applicationName(newApplication.getName())
                                         .urls(newApplication.getUrls())
                                         .initialInstanceCount(0)
                                         .build())
              .totalPreviousInstanceCount(totalPreviousInstanceCount)
              .downsizeDetails(downsizeAppDetails)
              .build();

      // Delete downloaded artifact and generated manifest.yaml file
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(manifestYamlFile, artifactFile));

      executionLogCallback.saveExecutionLog("\n ----------  PCF Setup process completed successfully");
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(pcfSetupCommandResponse.getCommandExecutionStatus())
          .errorMessage(pcfSetupCommandResponse.getOutput())
          .pcfCommandResponse(pcfSetupCommandResponse)
          .build();

    } catch (RuntimeException | PivotalClientApiException | IOException | ExecutionException e) {
      logger.error(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Setup task [{}]", pcfCommandSetupRequest, e);
      executionLogCallback.saveExecutionLog("\n\n ----------  PCF Setup process failed to complete successfully");
      Misc.logAllMessages(e, executionLogCallback);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(e))
          .build();
    }
  }

  /**
   * 1. First Delete all apps  with 0 instance count
   * 2. Now process apps with non-zero apps.
   * 3. Based on count "LastVersopAppsToKeep" provided by user, (default is 3)
   * 4. Keep most recent app as is, and (last LastVersopAppsToKeep - 1) apps will be downsized to 0
   * 5. All apps older than that will be deleted
   *
   * @param previousReleases
   * @param pcfRequestConfig
   */
  @VisibleForTesting
  void deleteOlderApplications(List<ApplicationSummary> previousReleases, PcfRequestConfig pcfRequestConfig,
      PcfCommandSetupRequest pcfCommandSetupRequest) {
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
      appsWithZeroInstances.forEach(
          applicationSummary -> { deleteApplication(applicationSummary, pcfRequestConfig, appsDeleted); });
    }

    List<ApplicationSummary> appsWithNonZeroInstances =
        previousReleases.stream()
            .filter(applicationSummary -> applicationSummary.getInstances().intValue() > 0)
            .sorted(comparingInt(
                applicationSummary -> pcfCommandTaskHelper.getRevisionFromReleaseName(applicationSummary.getName())))
            .collect(toList());

    logApplicationsRetentionsMsg(appsWithNonZeroInstances, olderActiveVersionCountToKeep);

    // At this point, all apps with 0 instances have been deleted.
    // Now, we need to keep "olderActiveVersionCountToKeep" no of apps.
    // We will keep most recent one as is, and downsize olderActiveVersionCountToKeep - 1
    // apps to 0, so they will be deleted in next deployment.
    if (isNotEmpty(appsWithNonZeroInstances) && appsWithNonZeroInstances.size() > 1) {
      int appsDownsizedCount = 0;
      for (int index = appsWithNonZeroInstances.size() - 2; index >= 0; index--) {
        ApplicationSummary applicationSummary = appsWithNonZeroInstances.get(index);
        if (appsDownsizedCount < olderActiveVersionCountToKeep - 1) {
          downsizeApplicationToZero(applicationSummary, pcfRequestConfig);
          appsDownsizedCount++;
        } else {
          deleteApplication(applicationSummary, pcfRequestConfig, appsDeleted);
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

  private void logApplicationsRetentionsMsg(
      List<ApplicationSummary> appsWithNonZeroInstances, Integer olderActiveVersionCountToKeep) {
    if (isEmpty(appsWithNonZeroInstances)) {
      return;
    }

    executionLogCallback.saveExecutionLog("\n# Processing Apps with Non-Zero Instances");
    executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                              .append("# No Change For Most Recent Application: ")
                                              .append(appsWithNonZeroInstances.get(0).getName())
                                              .toString());

    for (int i = 1; i < appsWithNonZeroInstances.size(); i++) {
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
    executionLogCallback.saveExecutionLog("");
  }

  private void deleteApplication(
      ApplicationSummary applicationSummary, PcfRequestConfig pcfRequestConfig, Set<String> appsDeleted) {
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

  private void downsizeApplicationToZero(ApplicationSummary applicationSummary, PcfRequestConfig pcfRequestConfig) {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("# Application Being Downsized To 0: ")
                                              .append(applicationSummary.getName())
                                              .toString());
    pcfRequestConfig.setApplicationName(applicationSummary.getName());
    pcfRequestConfig.setDesiredCount(0);
    try {
      pcfDeploymentManager.resizeApplication(pcfRequestConfig);
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
