package software.wings.delegatetasks.pcf;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import io.harness.filesystem.FileIo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Stateles helper class
 */
@Singleton
public class PcfCommandTaskHelper {
  private static final String PCF_ARTIFACT_DOWNLOAD_DIR_PATH = "./repository/pcfartifacts";
  private static final String REPOSITORY_DIR_PATH = "./repository";
  private static final String IMAGE_FILE_LOCATION_PLACEHOLDER = "\\$\\{FILE_LOCATION}";
  private static final String APPLICATION_NAME_PLACEHOLDER = "\\$\\{APPLICATION_NAME}";
  private static final String INSTANCE_COUNT_PLACEHOLDER = "\\$\\{INSTANCE_COUNT}";

  public static final String DELIMITER = "__";

  @Inject private DelegateFileManager delegateFileManager;
  @Inject private PcfDeploymentManager pcfDeploymentManager;

  private static final Logger logger = LoggerFactory.getLogger(PcfCommandTaskHelper.class);

  /**
   * Returns Application names those will be downsized in deployment process
   */
  public List<PcfAppSetupTimeDetails> generateDownsizeDetails(
      PcfRequestConfig pcfRequestConfig, String releaseName, Integer maxCount) throws PivotalClientApiException {
    String prefix = getAppPrefix(releaseName);

    List<ApplicationSummary> applicationSummaries =
        pcfDeploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);

    List<PcfAppSetupTimeDetails> downSizeUpdate = new ArrayList<>();
    int count = maxCount;
    int instanceCount;
    for (int index = applicationSummaries.size() - 1; index >= 0; index--) {
      if (count <= 0) {
        break;
      }

      ApplicationSummary applicationSummary = applicationSummaries.get(index);
      if (releaseName.equals(applicationSummary.getName()) || applicationSummary.getInstances() == 0) {
        continue;
      }
      instanceCount = applicationSummary.getInstances();
      downSizeUpdate.add(PcfAppSetupTimeDetails.builder()
                             .applicationGuid(applicationSummary.getId())
                             .applicationName(applicationSummary.getName())
                             .urls(applicationSummary.getUrls())
                             .initialInstanceCount(applicationSummary.getInstances())
                             .build());
      count = instanceCount >= count ? 0 : count - instanceCount;
    }

    return downSizeUpdate;
  }

  public void upsizeListOfInstances(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, List<PcfServiceData> upsizeList, List<PcfInstanceElement> pcfInstanceElements,
      List<String> routeMaps) throws PivotalClientApiException {
    for (PcfServiceData pcfServiceData : upsizeList) {
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("# Upsizing application:")
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(pcfServiceData.getName())
                                                .append("\nCURRENT-INSTANCE-COUNT: ")
                                                .append(pcfServiceData.getPreviousCount())
                                                .append("\nDESIRED-INSTANCE-COUNT: ")
                                                .append(pcfServiceData.getDesiredCount())
                                                .toString());
      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());
      upsizeInstance(pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated,
          pcfInstanceElements, routeMaps);
      pcfServiceDataUpdated.add(pcfServiceData);
    }
  }

  public void downSizeListOfInstances(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, List<PcfServiceData> downSizeList, List<String> routeMaps)
      throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("\n");
    for (PcfServiceData pcfServiceData : downSizeList) {
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("# Downsizing application:")
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(pcfServiceData.getName())
                                                .append("\nCURRENT-INSTANCE-COUNT: ")
                                                .append(pcfServiceData.getPreviousCount())
                                                .append("\nDESIRED-INSTANCE-COUNT: ")
                                                .append(pcfServiceData.getDesiredCount())
                                                .toString());

      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());
      downSize(pcfServiceData, executionLogCallback, pcfRequestConfig, pcfDeploymentManager, routeMaps);
      pcfServiceDataUpdated.add(pcfServiceData);
    }
  }

  public ApplicationDetail getNewlyCreatedApplication(PcfRequestConfig pcfRequestConfig,
      PcfCommandDeployRequest pcfCommandDeployRequest, PcfDeploymentManager pcfDeploymentManager)
      throws PivotalClientApiException {
    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getNewReleaseName());
    pcfRequestConfig.setDesiredCount(pcfCommandDeployRequest.getUpdateCount());
    return pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
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
   * @param pcfCommandDeployRequest
   * @param pcfRequestConfig
   * @param executionLogCallback
   * @param pcfServiceDataUpdated
   * @param updateCount
   * @param prefix
   * @param pcfInstanceElements
   * @throws PivotalClientApiException
   */
  public void downsizePreviousReleases(PcfCommandDeployRequest pcfCommandDeployRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback,
      List<PcfServiceData> pcfServiceDataUpdated, Integer updateCount, String prefix,
      List<PcfInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("# Downsizing previous application version/s");

    List<ApplicationSummary> applicationSummaries =
        pcfDeploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);

    List<PcfServiceData> downSizeUpdate = new ArrayList<>();
    int count = updateCount;
    int instanceCount;

    if (updateCount == 0) {
      // If 0 instances are to be downsized in this stage, then find one of the previous applications, to be downsized
      // and return guid of that application, so verification phase can use that guid.
      getGuidForFirstAppToBeDownsized(
          pcfCommandDeployRequest, pcfInstanceElements, applicationSummaries, pcfRequestConfig);
    } else {
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
                                                  .append("APPLICATION-NAME: ")
                                                  .append(applicationSummary.getName())
                                                  .append("\nCURRENT-INSTANCE-COUNT: ")
                                                  .append(applicationSummary.getInstances())
                                                  .append("\nDESIRED-INSTANCE-COUNT: ")
                                                  .append(newCount)
                                                  .toString());

        PcfServiceData pcfServiceData = PcfServiceData.builder()
                                            .name(applicationSummary.getName())
                                            .id(applicationSummary.getId())
                                            .previousCount(applicationSummary.getInstances())
                                            .desiredCount(newCount)
                                            .build();
        downSizeUpdate.add(pcfServiceData);

        // downsize application
        ApplicationDetail applicationDetailAfterResize = downSize(pcfServiceData, executionLogCallback,
            pcfRequestConfig, pcfDeploymentManager, pcfCommandDeployRequest.getRouteMaps());

        // Application that is downsized
        if (EmptyPredicate.isNotEmpty(applicationDetailAfterResize.getInstanceDetails())) {
          applicationDetailAfterResize.getInstanceDetails().forEach(instance
              -> pcfInstanceElements.add(PcfInstanceElement.builder()
                                             .applicationId(applicationDetailAfterResize.getId())
                                             .displayName(applicationDetailAfterResize.getName())
                                             .instanceIndex(instance.getIndex())
                                             .isUpsize(false)
                                             .build()));
        }

        count = instanceCount >= count ? 0 : count - instanceCount;
      }
    }

    pcfServiceDataUpdated.addAll(downSizeUpdate);
  }

  private void getGuidForFirstAppToBeDownsized(PcfCommandDeployRequest pcfCommandDeployRequest,
      List<PcfInstanceElement> pcfInstanceElements, List<ApplicationSummary> applicationSummaries,
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    for (int index = applicationSummaries.size() - 1; index >= 0; index--) {
      ApplicationSummary applicationSummary = applicationSummaries.get(index);
      if (pcfCommandDeployRequest.getNewReleaseName().equals(applicationSummary.getName())) {
        continue;
      } else {
        pcfRequestConfig.setApplicationName(applicationSummary.getName());
        ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
        if (EmptyPredicate.isNotEmpty(applicationDetail.getInstanceDetails())) {
          applicationDetail.getInstanceDetails().forEach(instanceDetail
              -> pcfInstanceElements.add(PcfInstanceElement.builder()
                                             .displayName(applicationDetail.getName())
                                             .applicationId(applicationDetail.getId())
                                             .instanceIndex(instanceDetail.getIndex())
                                             .build()));
        }

        break;
      }
    }
  }

  public ApplicationDetail printApplicationDetail(
      ApplicationDetail applicationDetail, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("NAME: ")
                                              .append(applicationDetail.getName())
                                              .append("\nINSTANCE-COUNT: ")
                                              .append(applicationDetail.getInstances())
                                              .append("\nROUTES: ")
                                              .append(applicationDetail.getUrls())
                                              .append("\n")
                                              .toString());
    return applicationDetail;
  }

  public void printInstanceDetails(ExecutionLogCallback executionLogCallback, List<InstanceDetail> instances) {
    StringBuilder builder = new StringBuilder("Instance Details:");
    instances.forEach(instance
        -> builder.append("\nIndex: ")
               .append(instance.getIndex())
               .append("\nState: ")
               .append(instance.getState())
               .append("\nDisk Usage: ")
               .append(instance.getDiskUsage())
               .append("\nCPU: ")
               .append(instance.getCpu())
               .append("\nMemory Usage: ")
               .append(instance.getMemoryUsage())
               .append("\n"));
    executionLogCallback.saveExecutionLog(builder.toString());
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public File downloadArtifact(List<ArtifactFile> artifactFiles, String activityId, String accountId)
      throws IOException, ExecutionException {
    List<Pair<String, String>> fileIds = Lists.newArrayList();
    artifactFiles.forEach(artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));

    // check if repository exists, if not create it
    FileIo.createDirectoryIfDoesNotExist(REPOSITORY_DIR_PATH);
    FileIo.createDirectoryIfDoesNotExist(PCF_ARTIFACT_DOWNLOAD_DIR_PATH);
    File dir = new File(PCF_ARTIFACT_DOWNLOAD_DIR_PATH);

    InputStream inputStream =
        delegateFileManager.downloadArtifactByFileId(FileBucket.ARTIFACTS, fileIds.get(0).getKey(), accountId, false);

    String fileName = System.currentTimeMillis() + artifactFiles.get(0).getName();
    File artifactFile = new File(dir.getAbsolutePath() + "/" + fileName);
    artifactFile.createNewFile();
    IOUtils.copy(inputStream, new FileOutputStream(artifactFile));
    inputStream.close();
    return artifactFile;
  }

  private ApplicationDetail downSize(PcfServiceData pcfServiceData, ExecutionLogCallback executionLogCallback,
      PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager, List<String> routePaths)
      throws PivotalClientApiException {
    pcfRequestConfig.setApplicationName(pcfServiceData.getName());
    pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());

    ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(pcfRequestConfig);

    executionLogCallback.saveExecutionLog("# Downsizing successful");
    executionLogCallback.saveExecutionLog("\n# App details after downsize:");
    printApplicationDetail(applicationDetail, executionLogCallback);
    return applicationDetail;
  }

  public void deleteCreataedFile(List<File> files) {
    files.forEach(file -> file.delete());
  }

  public String getAppPrefix(String appName) {
    return appName.substring(0, appName.lastIndexOf(DELIMITER) + DELIMITER.length());
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

  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
  public File createManifestYamlFileLocally(
      PcfCommandSetupRequest pcfCommandSetupRequest, String tempPath, String releaseName) throws IOException {
    String manifestYaml = pcfCommandSetupRequest.getManifestYaml();

    manifestYaml = manifestYaml.replaceAll(APPLICATION_NAME_PLACEHOLDER, releaseName)
                       .replaceAll(IMAGE_FILE_LOCATION_PLACEHOLDER, tempPath)
                       .replaceAll(INSTANCE_COUNT_PLACEHOLDER, "0");

    String directoryPath = getPcfArtifactDownloadDirPath();
    // check if repository exists, if not create it
    FileIo.createDirectoryIfDoesNotExist(REPOSITORY_DIR_PATH);
    FileIo.createDirectoryIfDoesNotExist(directoryPath);
    File dir = new File(directoryPath);

    File manifestFile = getManifestFile(releaseName, dir);
    manifestFile.createNewFile();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFile))) {
      writer.write(manifestYaml);
    }
    return manifestFile;
  }

  public String getPcfArtifactDownloadDirPath() {
    return PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
  }

  public File getManifestFile(String releaseName, File dir) {
    return new File(dir.getAbsolutePath() + "/" + releaseName + System.currentTimeMillis() + ".yml");
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
   * @param pcfInstanceElements
   * @throws PivotalClientApiException
   */
  public void upsizeNewApplication(ExecutionLogCallback executionLogCallback, PcfDeploymentManager pcfDeploymentManager,
      PcfCommandDeployRequest pcfCommandDeployRequest, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, ApplicationDetail details, Integer stepIncrease,
      List<PcfInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("# Upsizing new application, ");

    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("APPLICATION-NAME: ")
                                              .append(details.getName())
                                              .append("\nCURRENT-INSTANCE-COUNT: ")
                                              .append(details.getInstances())
                                              .append("\nDESIRED-INSTANCE-COUNT: ")
                                              .append(pcfCommandDeployRequest.getUpdateCount())
                                              .toString());

    // Upscale new app
    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getNewReleaseName());
    pcfRequestConfig.setDesiredCount(pcfCommandDeployRequest.getUpdateCount());

    // perform upsize
    upsizeInstance(pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated,
        pcfInstanceElements, pcfCommandDeployRequest.getRouteMaps());
  }

  private void upsizeInstance(PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager,
      ExecutionLogCallback executionLogCallback, List<PcfServiceData> pcfServiceDataUpdated,
      List<PcfInstanceElement> pcfInstanceElements, List<String> routeMaps) throws PivotalClientApiException {
    // Get application details before upsize
    ApplicationDetail detailsBeforeUpsize = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    StringBuilder sb = new StringBuilder();

    // create pcfServiceData having all details of this upsize operation
    pcfServiceDataUpdated.add(PcfServiceData.builder()
                                  .previousCount(detailsBeforeUpsize.getInstances())
                                  .desiredCount(pcfRequestConfig.getDesiredCount())
                                  .name(pcfRequestConfig.getApplicationName())
                                  .id(detailsBeforeUpsize.getId())
                                  .build());

    // upsize application
    ApplicationDetail detailsAfterUpsize = pcfDeploymentManager.resizeApplication(pcfRequestConfig);
    executionLogCallback.saveExecutionLog(sb.append("# Application upsized successfully ").toString());

    List<InstanceDetail> instances = detailsAfterUpsize.getInstanceDetails().stream().collect(toList());
    instances.forEach(instance
        -> pcfInstanceElements.add(PcfInstanceElement.builder()
                                       .applicationId(detailsAfterUpsize.getId())
                                       .displayName(detailsAfterUpsize.getName())
                                       .instanceIndex(instance.getIndex())
                                       .isUpsize(true)
                                       .build()));

    // Instance token is ApplicationGuid:InstanceIndex, that can be used to connect to instance from outside workd
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("\n# Application state details after upsize:  ").toString());
    printApplicationDetail(detailsAfterUpsize, executionLogCallback);
    printInstanceDetails(executionLogCallback, instances);
  }

  public void mapRouteMaps(String applicationName, List<String> routes, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("\n# Adding Routs");
    executionLogCallback.saveExecutionLog("APPLICATION: " + applicationName);
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(routes));
    // map
    pcfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.mapRouteMapForApplication(pcfRequestConfig, routes);
  }

  public void unmapExistingRouteMaps(String applicationName, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    executionLogCallback.saveExecutionLog("\n# Unmapping routes");
    executionLogCallback.saveExecutionLog("APPLICATION: " + applicationName);
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(applicationDetail.getUrls()));
    // map
    pcfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, applicationDetail.getUrls());
  }

  public void unmapRouteMaps(String applicationName, List<String> routes, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("\n# Unmapping Routes");
    executionLogCallback.saveExecutionLog("APPLICATION: " + applicationName);
    executionLogCallback.saveExecutionLog("ROUTES: \n[" + getRouteString(routes));
    // unmap
    pcfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, routes);
    executionLogCallback.saveExecutionLog("# Unmapping Routes was successfully completed");
  }

  private String getRouteString(List<String> routeMaps) {
    if (EmptyPredicate.isEmpty(routeMaps)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    routeMaps.forEach(routeMap -> builder.append("\n").append(routeMap));
    builder.append("\n]");
    return builder.toString();
  }
}
