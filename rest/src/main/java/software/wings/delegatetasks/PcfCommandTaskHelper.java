package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.EncryptionService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Stateles helper class
 */
@Singleton
public class PcfCommandTaskHelper {
  private static final String PCF_ARTIFACT_DOWNLOAD_DIR_PATH = "./repository/pcfartifacts";
  private static final String IMAGE_FILE_LOCATION_PLACEHOLDER = "\\$\\{FILE_LOCATION}";
  private static final String APPLICATION_NAME_PLACEHOLDER = "\\$\\{APPLICATION_NAME}";
  private static final String INSTANCE_COUNT_PLACEHOLDER = "\\$\\{INSTANCE_COUNT}";
  private int MAX_RELEASE_VERSIONS_TO_KEEP = 3;
  public static final String DELIMITER = "__";

  private static final Logger logger = LoggerFactory.getLogger(PcfCommandTaskHelper.class);
  /**
   * This method is responsible for fething previous release version information
   * like, previous releaseNames with Running instances, All existing previous releaseNames.
   *
   * @param pcfCommandRequest
   * @param executionLogCallback
   * @param encryptionService
   * @param delegateFileManager
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse performSetup(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, EncryptionService encryptionService,
      PcfDeploymentManager pcfDeploymentManager, DelegateFileManager delegateFileManager,
      List<EncryptedDataDetail> encryptedDataDetails) {
    executionLogCallback.saveExecutionLog("---------- Starting PCF Setup Command");

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

      Integer totalPreviousInstanceCount = previousReleases.stream().mapToInt(ApplicationSummary::getInstances).sum();

      // Get new Revision version, @TODO Add prefix here
      int releaseRevision = CollectionUtils.isEmpty(previousReleases)
          ? 0
          : getRevisionFromReleaseName(previousReleases.get(previousReleases.size() - 1).getName()) + 1;

      // Keep only recent 3 releases, delete all previous ones
      if (previousReleases.size() > MAX_RELEASE_VERSIONS_TO_KEEP) {
        executionLogCallback.saveExecutionLog(
            "\n# Trying to delete any applications with 0 instances older than most recent 3");

        int maxVersionToKeep = releaseRevision - MAX_RELEASE_VERSIONS_TO_KEEP;

        int countDeleted = 0;
        for (int index = 0; index < previousReleases.size(); index++) {
          ApplicationSummary applicationSummary = previousReleases.get(index);
          String previousAppName = applicationSummary.getName();
          if (applicationSummary.getInstances() == 0) {
            if (getRevisionFromReleaseName(previousAppName) >= maxVersionToKeep) {
              break;
            }

            executionLogCallback.saveExecutionLog("# Deleting previous application: ");
            executionLogCallback.saveExecutionLog(new StringBuilder()
                                                      .append("Name:- ")
                                                      .append(applicationSummary.getName())
                                                      .append("\n")
                                                      .append("Guid:- ")
                                                      .append(applicationSummary.getId())
                                                      .append("\n")
                                                      .append("InstanceCount:- ")
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
          executionLogCallback.saveExecutionLog("# No applications were eligible for deletion");
        }
      }

      // New appName to be created
      String newReleaseName = new StringBuilder()
                                  .append(pcfCommandSetupRequest.getReleaseNamePrefix())
                                  .append(DELIMITER)
                                  .append(releaseRevision)
                                  .toString();

      // Download artifact on delegate from manager
      File artifactFile = downloadArtifact(pcfCommandSetupRequest.getArtifactFiles(),
          pcfCommandSetupRequest.getActivityId(), delegateFileManager, pcfCommandSetupRequest.getAccountId());

      // Create manifest.yaml file
      File manifestYamlFile =
          createManifestYamlFileLocally(pcfCommandSetupRequest, artifactFile.getAbsolutePath(), newReleaseName);

      // Create new Application
      executionLogCallback.saveExecutionLog("# Creating new Application: " + newReleaseName);
      pcfRequestConfig.setApplicationName(newReleaseName);
      pcfRequestConfig.setRouteMaps(pcfCommandSetupRequest.getRouteMaps());
      pcfRequestConfig.setServiceVariables(pcfCommandSetupRequest.getServiceVariables());
      pcfRequestConfig.setSafeDisplayServiceVariables(pcfCommandSetupRequest.getSafeDisplayServiceVariables());

      ApplicationDetail newApplication =
          pcfDeploymentManager.createApplication(pcfRequestConfig, manifestYamlFile.getAbsolutePath());

      executionLogCallback.saveExecutionLog("# Application created successfully");
      printApplicationDetail(newApplication, executionLogCallback);

      PcfSetupCommandResponse pcfSetupCommandResponse = PcfSetupCommandResponse.builder()
                                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                            .output(StringUtils.EMPTY)
                                                            .newApplicationId(newApplication.getId())
                                                            .newApplicationName(newApplication.getName())
                                                            .totalPreviousInstanceCount(totalPreviousInstanceCount)
                                                            .build();

      // Delete downloaded artifact and generated manifest.yaml file
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      deleteCreataedFile(Arrays.asList(manifestYamlFile, artifactFile));

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
      executionLogCallback.saveExecutionLog("# Error: " + e.getMessage());
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  /**
   * This method is responsible for upsizing new application instances and downsizing previous application instances.
   * @param pcfCommandRequest
   * @param executionLogCallback
   * @param encryptionService
   * @param pcfDeploymentManager
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse performDeploy(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, EncryptionService encryptionService,
      PcfDeploymentManager pcfDeploymentManager, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfCommandDeployRequest pcfCommandDeployRequest = (PcfCommandDeployRequest) pcfCommandRequest;

    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().instanceTokens(new ArrayList<>()).build();
    try {
      executionLogCallback.saveExecutionLog("\n---------- Starting PCF Resize Command");

      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(pcfCommandDeployRequest.getOrganization())
                                              .spaceName(pcfCommandDeployRequest.getSpace())
                                              .timeOutIntervalInMins(pcfCommandDeployRequest.getTimeoutIntervalInMin())
                                              .build();

      ApplicationDetail details =
          getNewlyCreatedApplication(pcfRequestConfig, pcfCommandDeployRequest, pcfDeploymentManager);
      // No of instances to be added to newly created application in this deploy stage
      Integer stepIncrease = pcfCommandDeployRequest.getUpdateCount() - details.getInstances();
      List<String> instanceTokens = new ArrayList<>();

      //  pcfCommandDeployRequest.getDownSizeCount() total instances to take down at this phase
      // so no instances to take down = total - total instances taken down till now ()
      // (which is = total instances created in new service)
      List<ApplicationSummary> previousReleases =
          pcfDeploymentManager.getPreviousReleases(pcfRequestConfig, getAppPrefix(details.getName()));
      previousReleases = previousReleases.stream()
                             .filter(applicationSummary -> !applicationSummary.getName().equals(details.getName()))
                             .collect(toList());
      Integer instanceCountForPreviousReleases =
          previousReleases.stream().mapToInt(ApplicationSummary::getInstances).sum();
      Integer stepDecrease = pcfCommandDeployRequest.getDownSizeCount()
          - (pcfCommandDeployRequest.getTotalPreviousInstanceCount() - instanceCountForPreviousReleases);

      // downsize previous apps with non zero instances by same count new app was upsized
      if (ResizeStrategy.DOWNSIZE_OLD_FIRST.equals(pcfCommandDeployRequest.getResizeStrategy())) {
        downsizePreviousReleases(pcfDeploymentManager, pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, getAppPrefix(pcfCommandDeployRequest.getNewReleaseName()));

        upsizeNewApplication(executionLogCallback, pcfDeploymentManager, pcfCommandDeployRequest, pcfServiceDataUpdated,
            pcfRequestConfig, details, stepIncrease, instanceTokens);
      } else {
        upsizeNewApplication(executionLogCallback, pcfDeploymentManager, pcfCommandDeployRequest, pcfServiceDataUpdated,
            pcfRequestConfig, details, stepIncrease, instanceTokens);

        downsizePreviousReleases(pcfDeploymentManager, pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, getAppPrefix(pcfCommandDeployRequest.getNewReleaseName()));
      }
      // generate response to be sent back to Manager
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getInstanceTokens().addAll(instanceTokens);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Resize completed successfully");
    } catch (Exception e) {
      logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Deploy task [{}]",
          pcfCommandDeployRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Resize failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error:- " + e.getMessage());
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfDeployCommandResponse.setOutput(e.getMessage());
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }

  /**
   * This method performs Rollback operation
   * @param pcfCommandRequest
   * @param executionLogCallback
   * @param encryptionService
   * @param pcfDeploymentManager
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse performDeployRollback(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, EncryptionService encryptionService,
      PcfDeploymentManager pcfDeploymentManager, List<EncryptedDataDetail> encryptedDataDetails) {
    executionLogCallback.saveExecutionLog("--------- Starting Rollback deployment");
    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().instanceTokens(new ArrayList<>()).build();

    PcfCommandRollbackRequest commandRollbackRequest = (PcfCommandRollbackRequest) pcfCommandRequest;

    try {
      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);
      if (CollectionUtils.isEmpty(commandRollbackRequest.getInstanceData())) {
        commandRollbackRequest.setInstanceData(new ArrayList<>());
      }

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(commandRollbackRequest.getOrganization())
                                              .spaceName(commandRollbackRequest.getSpace())
                                              .timeOutIntervalInMins(commandRollbackRequest.getTimeoutIntervalInMin())
                                              .build();

      // get Upsize Instance data
      List<PcfServiceData> upsizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(pcfServiceData -> pcfServiceData.getDesiredCount() > pcfServiceData.getPreviousCount())
              .collect(toList());

      // get Downsize Instance data
      List<PcfServiceData> downSizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(pcfServiceData -> pcfServiceData.getDesiredCount() < pcfServiceData.getPreviousCount())
              .collect(toList());

      List<String> instanceTokens = new ArrayList<>();
      if (ResizeStrategy.DOWNSIZE_OLD_FIRST.equals(commandRollbackRequest.getResizeStrategy())) {
        downSizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfRequestConfig, downSizeList,
            commandRollbackRequest.getRouteMaps());
        upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated, pcfRequestConfig,
            upsizeList, instanceTokens, commandRollbackRequest.getRouteMaps());
      } else {
        upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated, pcfRequestConfig,
            upsizeList, instanceTokens, commandRollbackRequest.getRouteMaps());
        downSizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfRequestConfig, downSizeList,
            commandRollbackRequest.getRouteMaps());
      }

      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getInstanceTokens().addAll(instanceTokens);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback completed successfully");
    } catch (Exception e) {
      logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Rollback task [{}]",
          commandRollbackRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error:- " + e.getMessage());
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.setOutput(e.getMessage());
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }

  public PcfCommandExecutionResponse performValidation(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, PcfDeploymentManager pcfDeploymentManager) {
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    try {
      pcfDeploymentManager.getOrganizations(
          PcfRequestConfig.builder()
              .orgName(pcfInfraMappingDataRequest.getOrganization())
              .userName(pcfConfig.getUsername())
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
              .build());

      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    } catch (PivotalClientApiException e) {
      logger.error("Exception in processing PCF DataFetch task [{}]", pcfInfraMappingDataRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Datafetch failed to complete successfully");
      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfCommandExecutionResponse.setErrorMessage(e.getMessage());
    }

    return pcfCommandExecutionResponse;
  }

  public PcfCommandExecutionResponse performDataFetch(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, EncryptionService encryptionService,
      PcfDeploymentManager pcfDeploymentManager, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);

    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    PcfInfraMappingDataResponse pcfInfraMappingDataResponse = PcfInfraMappingDataResponse.builder().build();
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInfraMappingDataResponse);

    try {
      if (StringUtils.isBlank(pcfInfraMappingDataRequest.getOrganization())) {
        getOrgs(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      } else if (StringUtils.isBlank(pcfInfraMappingDataRequest.getSpace())) {
        getSpaces(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      } else {
        getRoutes(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      }

      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInfraMappingDataResponse.setOutput(StringUtils.EMPTY);
    } catch (PivotalClientApiException e) {
      logger.error("Exception in processing PCF DataFetch task [{}]", pcfInfraMappingDataRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Datafetch failed to complete successfully");
      pcfInfraMappingDataResponse.setOrganizations(Collections.EMPTY_LIST);
      pcfInfraMappingDataResponse.setSpaces(Collections.EMPTY_LIST);
      pcfInfraMappingDataResponse.setRouteMaps(Collections.EMPTY_LIST);
      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInfraMappingDataResponse.setOutput(e.getMessage());
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfInfraMappingDataResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfInfraMappingDataResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  private void upsizeListOfInstances(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, List<PcfServiceData> upsizeList, List<String> instanceTokens,
      List<String> routeMaps) throws PivotalClientApiException {
    for (PcfServiceData pcfServiceData : upsizeList) {
      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());
      upsizeInstance(pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated,
          instanceTokens, routeMaps);
    }
  }

  private void downSizeListOfInstances(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, PcfRequestConfig pcfRequestConfig, List<PcfServiceData> downSizeList,
      List<String> routeMaps) throws PivotalClientApiException {
    for (PcfServiceData pcfServiceData : downSizeList) {
      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());
      downSize(pcfServiceData, executionLogCallback, pcfRequestConfig, pcfDeploymentManager, routeMaps);
    }
  }

  private ApplicationDetail getNewlyCreatedApplication(PcfRequestConfig pcfRequestConfig,
      PcfCommandDeployRequest pcfCommandDeployRequest, PcfDeploymentManager pcfDeploymentManager)
      throws PivotalClientApiException {
    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getNewReleaseName());
    pcfRequestConfig.setDesiredCount(pcfCommandDeployRequest.getUpdateCount());
    return pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
  }

  /**
   * This is called from Deploy (Resize) phase.
   * @param executionLogCallback
   * @param pcfDeploymentManager
   * @param pcfCommandDeployRequest
   * @param pcfServiceDataUpdated
   * @param pcfRequestConfig
   * @param details
   * @param stepIncrease
   * @param instanceTokens
   * @throws PivotalClientApiException
   */
  private void upsizeNewApplication(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, PcfCommandDeployRequest pcfCommandDeployRequest,
      List<PcfServiceData> pcfServiceDataUpdated, PcfRequestConfig pcfRequestConfig, ApplicationDetail details,
      Integer stepIncrease, List<String> instanceTokens) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("# Upsizing new application, ");

    printApplicationDetail(details, executionLogCallback);
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("Adding ")
                                              .append(stepIncrease)
                                              .append(" more instances to application ")
                                              .append(details.getName())
                                              .toString());
    // Upscale new app
    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getNewReleaseName());
    pcfRequestConfig.setDesiredCount(pcfCommandDeployRequest.getUpdateCount());

    // perform upsize
    upsizeInstance(pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated, instanceTokens,
        pcfCommandDeployRequest.getRouteMaps());
  }

  /**
   * e.g. Downsize by 5,
   * Find out previous apps with non zero instances.
   * Process apps in descending order of versions.
   * keep processing till total counts taken down become 5
   * e.g. app_serv_env__5 is new app created,
   * app_serv_env__4   : 3
   * app_serv_env__3   : 3
   * app_serv_env__2   : 1
   * <p>
   * After this method, it will be
   * app_serv_env__4   : 0
   * app_serv_env__3   : 1
   * app_serv_env__2   : 1
   *
   * @param pcfDeploymentManager
   * @param pcfCommandDeployRequest
   * @param pcfRequestConfig
   * @param executionLogCallback
   * @param pcfServiceDataUpdated
   * @param updateCount
   * @param prefix
   * @throws PivotalClientApiException
   */
  private void downsizePreviousReleases(PcfDeploymentManager pcfDeploymentManager,
      PcfCommandDeployRequest pcfCommandDeployRequest, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback, List<PcfServiceData> pcfServiceDataUpdated, Integer updateCount,
      String prefix) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("# Downsizing previous version/s by count: " + updateCount);

    List<ApplicationSummary> applicationSummaries =
        pcfDeploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);

    List<PcfServiceData> downSizeUpdate = new ArrayList<>();
    int count = updateCount;
    int instanceCount;
    for (int index = applicationSummaries.size() - 1; index >= 0; index--) {
      if (count <= 0) {
        break;
      }

      ApplicationSummary applicationSummary = applicationSummaries.get(index);
      if (pcfCommandDeployRequest.getNewReleaseName().equals(applicationSummary.getName())) {
        continue;
      }

      instanceCount = applicationSummary.getInstances();
      int newCount = instanceCount <= count ? 0 : instanceCount - count;

      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("# Application: ")
                                                .append(applicationSummary.getName())
                                                .append(" has ")
                                                .append(applicationSummary.getInstances())
                                                .append(" instances")
                                                .toString());

      executionLogCallback.saveExecutionLog("# Downsizing application to " + newCount + " instances");

      PcfServiceData pcfServiceData = PcfServiceData.builder()
                                          .name(applicationSummary.getName())
                                          .previousCount(applicationSummary.getInstances())
                                          .desiredCount(newCount)
                                          .build();
      downSizeUpdate.add(pcfServiceData);

      // downsize application
      downSize(pcfServiceData, executionLogCallback, pcfRequestConfig, pcfDeploymentManager,
          pcfCommandDeployRequest.getRouteMaps());

      pcfRequestConfig.setDesiredCount(newCount);
      pcfRequestConfig.setApplicationName(applicationSummary.getName());
      pcfDeploymentManager.resizeApplication(pcfRequestConfig);

      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("# Application: ")
                                                .append(applicationSummary.getName())
                                                .append(" was successfully downsized to ")
                                                .append(newCount)
                                                .append(" instances")
                                                .toString());

      count = instanceCount >= count ? 0 : count - instanceCount;
    }

    pcfServiceDataUpdated.addAll(downSizeUpdate);
  }

  private ApplicationDetail printApplicationDetail(
      ApplicationDetail applicationDetail, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("Name:- ")
                                              .append(applicationDetail.getName())
                                              .append("\n")
                                              .append("Guid:- ")
                                              .append(applicationDetail.getId())
                                              .append("\n")
                                              .append("InstanceCount:- ")
                                              .append(applicationDetail.getInstances())
                                              .append("\n")
                                              .toString());
    return applicationDetail;
  }

  private void printInstanceDetails(ExecutionLogCallback executionLogCallback, List<InstanceDetail> instances) {
    StringBuilder builder = new StringBuilder("InstanceDetails:- \n");
    int count = 1;
    instances.stream().forEach(instance
        -> builder.append("\nInstance  ")
               .append(count)
               .append(",")
               .append("\nIndex:- ")
               .append(instance.getIndex())
               .append("\nState:- ")
               .append(instance.getState())
               .append("\nDisk Usage:- ")
               .append(instance.getDiskUsage())
               .append("\nCPU:- ")
               .append(instance.getCpu())
               .append("\nMemory Usage:- ")
               .append(instance.getMemoryUsage()));
    executionLogCallback.saveExecutionLog(builder.toString());
  }

  File downloadArtifact(List<ArtifactFile> artifactFiles, String activityId, DelegateFileManager delegateFileManager,
      String accountId) throws IOException, ExecutionException {
    List<Pair<String, String>> fileIds = Lists.newArrayList();
    artifactFiles.forEach(artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));

    File dir = new File(PCF_ARTIFACT_DOWNLOAD_DIR_PATH);
    if (!dir.exists()) {
      dir.mkdir();
    }

    InputStream inputStream =
        delegateFileManager.downloadArtifactByFileId(FileBucket.ARTIFACTS, fileIds.get(0).getKey(), accountId, false);

    String fileName = System.currentTimeMillis() + artifactFiles.get(0).getName();
    File artifactFile = new File(dir + "/" + fileName);
    artifactFile.createNewFile();
    IOUtils.copy(inputStream, new FileOutputStream(artifactFile));
    inputStream.close();
    return artifactFile;
  }

  private void upsizeInstance(PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager,
      ExecutionLogCallback executionLogCallback, List<PcfServiceData> pcfServiceDataUpdated,
      List<String> instanceTokens, List<String> routeMaps) throws PivotalClientApiException {
    // Get application details before upsize
    ApplicationDetail detailsBeforeUpsize = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    StringBuilder sb = new StringBuilder();

    // create pcfServiceData having all details of this upsize operation
    pcfServiceDataUpdated.add(PcfServiceData.builder()
                                  .previousCount(detailsBeforeUpsize.getInstances())
                                  .desiredCount(pcfRequestConfig.getDesiredCount())
                                  .name(pcfRequestConfig.getApplicationName())
                                  .build());

    executionLogCallback.saveExecutionLog(sb.append("# Upsizing Applicaiton:- ")
                                              .append("Name:- ")
                                              .append(detailsBeforeUpsize.getName())
                                              .append("\n")
                                              .append("current instance count:- ")
                                              .append(detailsBeforeUpsize.getInstances())
                                              .toString());

    // upsize application
    ApplicationDetail detailsAfterUpsize = pcfDeploymentManager.resizeApplication(pcfRequestConfig);
    executionLogCallback.saveExecutionLog(sb.append("# Applicaiton was upsized successfully:- ")
                                              .append("Name:- ")
                                              .append(detailsAfterUpsize.getName())
                                              .append("\n")
                                              .append("current instance count:- ")
                                              .append(detailsBeforeUpsize.getInstances())
                                              .toString());

    List<InstanceDetail> instances = detailsAfterUpsize.getInstanceDetails().stream().collect(toList());
    instanceTokens.addAll(
        instances.stream().map(instance -> detailsBeforeUpsize.getId() + ":" + instance.getIndex()).collect(toList()));

    if (detailsBeforeUpsize.getInstances() == 0 && CollectionUtils.isEmpty(detailsBeforeUpsize.getUrls())) {
      executionLogCallback.saveExecutionLog(
          "# Adding routeMap to application: " + pcfRequestConfig.getApplicationName());
      pcfDeploymentManager.mapRouteMapForApplication(pcfRequestConfig, routeMaps);
      detailsAfterUpsize = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    }

    // Instance token is ApplicationGuid:InstanceIndex, that can be used to connect to instance from outside workd
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("# Current details for App: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .toString());
    printApplicationDetail(detailsAfterUpsize, executionLogCallback);
    printInstanceDetails(executionLogCallback, instances);
  }

  private void downSize(PcfServiceData pcfServiceData, ExecutionLogCallback executionLogCallback,
      PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager, List<String> routePaths)
      throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(new StringBuilder("\n# Downsizing: ")
                                              .append(pcfServiceData.getName())
                                              .append(", from InstanceCount: ")
                                              .append(pcfServiceData.getPreviousCount())
                                              .append(" to ")
                                              .append(pcfServiceData.getDesiredCount())
                                              .toString());

    pcfRequestConfig.setApplicationName(pcfServiceData.getName());
    pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());

    ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(pcfRequestConfig);
    if (applicationDetail.getInstances() == 0) {
      // unmap routemap for application as app has been downsized to 0
      executionLogCallback.saveExecutionLog("# Unmapping routeMaps for application as app has 0 instances");
      pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, routePaths);
      applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    }

    executionLogCallback.saveExecutionLog("# Resizing: " + pcfServiceData.getName() + " is done");
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("# Current details for App: ").append(pcfServiceData.getName()).toString());
    printApplicationDetail(applicationDetail, executionLogCallback);
  }

  public void deleteCreataedFile(List<File> files) {
    files.stream().forEach(file -> file.delete());
  }

  private String getAppPrefix(String appName) {
    int index = appName.indexOf("__");
    return appName.substring(0, index);
  }

  public static int getRevisionFromReleaseName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  public File createManifestYamlFileLocally(
      PcfCommandSetupRequest pcfCommandSetupRequest, String tempPath, String releaseName) throws IOException {
    String manifestYaml = pcfCommandSetupRequest.getManifestYaml();

    manifestYaml = manifestYaml.replaceAll(APPLICATION_NAME_PLACEHOLDER, releaseName)
                       .replaceAll(IMAGE_FILE_LOCATION_PLACEHOLDER, tempPath)
                       .replaceAll(INSTANCE_COUNT_PLACEHOLDER, "0");

    File dir = new File(getPcfArtifactDownloadDirPath());
    if (!dir.exists()) {
      dir.mkdir();
    }

    File manifestFile = getManifestFile(releaseName, dir);
    manifestFile.createNewFile();

    BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFile));
    writer.write(manifestYaml);
    writer.close();
    return manifestFile;
  }

  String getPcfArtifactDownloadDirPath() {
    return PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
  }

  File getManifestFile(String releaseName, File dir) {
    return new File(dir.getAbsolutePath() + "/" + releaseName + System.currentTimeMillis() + ".yml");
  }

  private void getRoutes(PcfDeploymentManager pcfDeploymentManager,
      PcfInfraMappingDataRequest pcfInfraMappingDataRequest, PcfInfraMappingDataResponse pcfInfraMappingDataResponse,
      PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> routes = pcfDeploymentManager.getRouteMaps(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .spaceName(pcfInfraMappingDataRequest.getSpace())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setRouteMaps(routes);
  }

  private void getSpaces(PcfDeploymentManager pcfDeploymentManager,
      PcfInfraMappingDataRequest pcfInfraMappingDataRequest, PcfInfraMappingDataResponse pcfInfraMappingDataResponse,
      PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> spaces = pcfDeploymentManager.getSpacesForOrganization(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .spaceName(pcfInfraMappingDataRequest.getSpace())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setSpaces(spaces);
  }

  private void getOrgs(PcfDeploymentManager pcfDeploymentManager, PcfInfraMappingDataRequest pcfInfraMappingDataRequest,
      PcfInfraMappingDataResponse pcfInfraMappingDataResponse, PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> orgs = pcfDeploymentManager.getOrganizations(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setOrganizations(orgs);
  }

  public PcfCommandExecutionResponse performValidation(PcfCommandRequest pcfCommandRequest,
      ExecutionLogCallback executionLogCallback, PcfDeploymentManager pcfDeploymentManager, Logger logger) {
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    try {
      pcfDeploymentManager.getOrganizations(
          PcfRequestConfig.builder()
              .orgName(pcfInfraMappingDataRequest.getOrganization())
              .userName(pcfConfig.getUsername())
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
              .build());

      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    } catch (PivotalClientApiException e) {
      logger.error("Exception in processing PCF DataFetch task [{}]", pcfInfraMappingDataRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Datafetch failed to complete successfully");
      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfCommandExecutionResponse.setErrorMessage(e.getMessage());
    }

    return pcfCommandExecutionResponse;
  }
}
