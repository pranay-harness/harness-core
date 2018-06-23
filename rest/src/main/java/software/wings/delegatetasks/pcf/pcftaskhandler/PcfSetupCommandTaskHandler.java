package software.wings.delegatetasks.pcf.pcftaskhandler;

import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfSetupCommandTaskHandler extends PcfCommandTaskHandler {
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;
  private static final Logger logger = LoggerFactory.getLogger(PcfSetupCommandTaskHandler.class);

  /**
   * This method is responsible for fetching previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   */
  @SuppressFBWarnings({"BC_UNCONFIRMED_CAST", "REC_CATCH_EXCEPTION"})
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
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
        previousReleases.stream().forEach(
            applicationSummary -> appNames.append("\n").append(applicationSummary.getName()));
        executionLogCallback.saveExecutionLog(appNames.toString());
      }

      Integer totalPreviousInstanceCount = previousReleases.stream().mapToInt(ApplicationSummary::getInstances).sum();

      // Get new Revision version
      int releaseRevision = CollectionUtils.isEmpty(previousReleases)
          ? 0
          : pcfCommandTaskHelper.getRevisionFromReleaseName(previousReleases.get(previousReleases.size() - 1).getName())
              + 1;

      // Keep only recent 3 releases, delete all previous ones
      if (previousReleases.size() > MAX_RELEASE_VERSIONS_TO_KEEP) {
        executionLogCallback.saveExecutionLog(
            "\n# Deleting any applications having 0 instances and version older than 3 recent versions");

        int maxVersionToKeep = releaseRevision - MAX_RELEASE_VERSIONS_TO_KEEP;

        int countDeleted = 0;
        for (int index = 0; index < previousReleases.size(); index++) {
          ApplicationSummary applicationSummary = previousReleases.get(index);
          String previousAppName = applicationSummary.getName();
          if (applicationSummary.getInstances() == 0) {
            if (pcfCommandTaskHelper.getRevisionFromReleaseName(previousAppName) >= maxVersionToKeep) {
              break;
            }

            executionLogCallback.saveExecutionLog("# Application being deleted: ");
            executionLogCallback.saveExecutionLog(new StringBuilder()
                                                      .append("NAME: ")
                                                      .append(applicationSummary.getName())
                                                      .append("\n")
                                                      .append("INSTANCE-COUNT: ")
                                                      .append(applicationSummary.getInstances())
                                                      .toString());
            pcfRequestConfig.setApplicationName(previousAppName);
            pcfDeploymentManager.deleteApplication(pcfRequestConfig);
            countDeleted++;
          }
        }

        if (countDeleted > 0) {
          executionLogCallback.saveExecutionLog("# Done Deleting older applications");
        } else {
          executionLogCallback.saveExecutionLog("\n# No applications were eligible for deletion");
        }
      }

      // New appName to be created
      String newReleaseName =
          ServiceVersionConvention.getServiceName(pcfCommandSetupRequest.getReleaseNamePrefix(), releaseRevision);

      // Download artifact on delegate from manager
      File artifactFile = pcfCommandTaskHelper.downloadArtifact(pcfCommandSetupRequest.getArtifactFiles(),
          pcfCommandSetupRequest.getActivityId(), pcfCommandSetupRequest.getAccountId());

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

      List<String> downsizeAppNames = pcfCommandTaskHelper.generateDownsizeDetails(
          pcfRequestConfig, newReleaseName, pcfCommandSetupRequest.getMaxCount());
      PcfSetupCommandResponse pcfSetupCommandResponse = PcfSetupCommandResponse.builder()
                                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                            .output(StringUtils.EMPTY)
                                                            .newApplicationId(newApplication.getId())
                                                            .newApplicationName(newApplication.getName())
                                                            .totalPreviousInstanceCount(totalPreviousInstanceCount)
                                                            .downsizeDetails(downsizeAppNames)
                                                            .build();

      // Delete downloaded artifact and generated manifest.yaml file
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      pcfCommandTaskHelper.deleteCreataedFile(Arrays.asList(manifestYamlFile, artifactFile));

      executionLogCallback.saveExecutionLog("\n ----------  PCF Setup process completed successfully");
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(pcfSetupCommandResponse.getCommandExecutionStatus())
          .errorMessage(pcfSetupCommandResponse.getOutput())
          .pcfCommandResponse(pcfSetupCommandResponse)
          .build();

    } catch (Exception e) {
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
}
