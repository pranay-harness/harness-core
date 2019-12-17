package software.wings.delegatetasks.pcf;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.BUILDPACKS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.BUILDPACK_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.COMMAND_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.CREATE_SERVICE_MANIFEST_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DISK_QUOTA_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DOCKER_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DOMAINS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ENV_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HEALTH_CHECK_HTTP_ENDPOINT_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HEALTH_CHECK_TYPE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HOSTS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HOST_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.INSTANCE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.MEMORY_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_HOSTNAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PATH_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.RANDOM_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.REPOSITORY_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_PATH_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.SERVICES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.STACK_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.TIMEOUT_MANIFEST_YML_ELEMENT;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.jetbrains.annotations.NotNull;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * Stateles helper class
 */
@Singleton
public class PcfCommandTaskHelper {
  private static final Yaml yaml;
  public static final String CURRENT_INSTANCE_COUNT = "CURRENT-INSTANCE-COUNT: ";
  public static final String DESIRED_INSTANCE_COUNT = "DESIRED-INSTANCE-COUNT: ";
  public static final String APPLICATION = "APPLICATION: ";

  static {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(FlowStyle.BLOCK);
    options.setExplicitStart(true);
    yaml = new Yaml(options);
  }

  public static final String DELIMITER = "__";

  @Inject private DelegateFileManager delegateFileManager;
  @Inject private PcfDeploymentManager pcfDeploymentManager;

  /**
   * Returns Application names those will be downsized in deployment process
   */
  public List<PcfAppSetupTimeDetails> generateDownsizeDetails(PcfRequestConfig pcfRequestConfig, String releaseName)
      throws PivotalClientApiException {
    String prefix = getAppPrefix(releaseName);

    List<ApplicationSummary> applicationSummaries =
        pcfDeploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);

    List<PcfAppSetupTimeDetails> downSizeUpdate = new ArrayList<>();
    for (int index = applicationSummaries.size() - 1; index >= 0; index--) {
      ApplicationSummary applicationSummary = applicationSummaries.get(index);
      if (releaseName.equals(applicationSummary.getName()) || applicationSummary.getInstances() == 0) {
        continue;
      }

      List<String> urls = new ArrayList<>();
      urls.addAll(applicationSummary.getUrls());
      downSizeUpdate.add(PcfAppSetupTimeDetails.builder()
                             .applicationGuid(applicationSummary.getId())
                             .applicationName(applicationSummary.getName())
                             .urls(urls)
                             .initialInstanceCount(applicationSummary.getInstances())
                             .build());
    }

    return downSizeUpdate;
  }

  public void upsizeListOfInstances(ExecutionLogCallback executionLogCallback,
      PcfDeploymentManager pcfDeploymentManager, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, List<PcfServiceData> upsizeList, List<PcfInstanceElement> pcfInstanceElements)
      throws PivotalClientApiException {
    for (PcfServiceData pcfServiceData : upsizeList) {
      executionLogCallback.saveExecutionLog(color("# Upsizing application:", White, Bold));
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(pcfServiceData.getName())
                                                .append("\n" + CURRENT_INSTANCE_COUNT)
                                                .append(pcfServiceData.getPreviousCount())
                                                .append("\n" + DESIRED_INSTANCE_COUNT)
                                                .append(pcfServiceData.getDesiredCount())
                                                .toString());
      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());
      upsizeInstance(
          pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated, pcfInstanceElements);
      pcfServiceDataUpdated.add(pcfServiceData);
    }
  }

  public void downSizeListOfInstances(ExecutionLogCallback executionLogCallback,
      List<PcfServiceData> pcfServiceDataUpdated, PcfRequestConfig pcfRequestConfig, List<PcfServiceData> downSizeList,
      PcfCommandRollbackRequest commandRollbackRequest, PcfAppAutoscalarRequestData appAutoscalarRequestData)
      throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("\n");
    for (PcfServiceData pcfServiceData : downSizeList) {
      executionLogCallback.saveExecutionLog(color("# Downsizing application:", White, Bold));
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(pcfServiceData.getName())
                                                .append("\n" + CURRENT_INSTANCE_COUNT)
                                                .append(pcfServiceData.getPreviousCount())
                                                .append("\n" + DESIRED_INSTANCE_COUNT)
                                                .append(pcfServiceData.getDesiredCount())
                                                .toString());

      pcfRequestConfig.setApplicationName(pcfServiceData.getName());
      pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());

      if (commandRollbackRequest.isUseAppAutoscalar()) {
        ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
        appAutoscalarRequestData.setApplicationName(applicationDetail.getName());
        appAutoscalarRequestData.setApplicationGuid(applicationDetail.getId());
        appAutoscalarRequestData.setExpectedEnabled(true);
        disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
      }

      downSize(pcfServiceData, executionLogCallback, pcfRequestConfig, pcfDeploymentManager);

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
   */
  public void downsizePreviousReleases(PcfCommandDeployRequest pcfCommandDeployRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback,
      List<PcfServiceData> pcfServiceDataUpdated, Integer updateCount, List<PcfInstanceElement> pcfInstanceElements,
      PcfAppAutoscalarRequestData appAutoscalarRequestData) throws PivotalClientApiException {
    if (pcfCommandDeployRequest.isStandardBlueGreen()) {
      executionLogCallback.saveExecutionLog("# BG Deployment. Old Application will not be downsized.");
      return;
    }

    executionLogCallback.saveExecutionLog("# Downsizing previous application version/s");

    PcfAppSetupTimeDetails downsizeAppDetails = pcfCommandDeployRequest.getDownsizeAppDetail();
    if (downsizeAppDetails == null) {
      executionLogCallback.saveExecutionLog("# No Application is available for downsize");
      return;
    }

    pcfRequestConfig.setApplicationName(downsizeAppDetails.getApplicationName());
    ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("APPLICATION-NAME: ")
                                              .append(applicationDetail.getName())
                                              .append("\nCURRENT-INSTANCE-COUNT: ")
                                              .append(applicationDetail.getInstances())
                                              .append("\nDESIRED-INSTANCE-COUNT: ")
                                              .append(updateCount)
                                              .toString());

    PcfServiceData pcfServiceData = PcfServiceData.builder()
                                        .name(applicationDetail.getName())
                                        .id(applicationDetail.getId())
                                        .previousCount(applicationDetail.getInstances())
                                        .desiredCount(updateCount)
                                        .build();

    pcfServiceDataUpdated.add(pcfServiceData);

    if (updateCount >= applicationDetail.getInstances()) {
      executionLogCallback.saveExecutionLog("# No Downsize was required.\n");
      return;
    }

    // First disable App Auto scalar if attached with application
    if (pcfCommandDeployRequest.isUseAppAutoscalar()) {
      appAutoscalarRequestData.setApplicationName(applicationDetail.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationDetail.getId());
      appAutoscalarRequestData.setExpectedEnabled(true);
      boolean autoscalarStateChanged = disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
      pcfServiceData.setDisableAutoscalarPerformed(autoscalarStateChanged);
    }

    ApplicationDetail applicationDetailAfterResize =
        downSize(pcfServiceData, executionLogCallback, pcfRequestConfig, pcfDeploymentManager);

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
  }

  public ApplicationDetail printApplicationDetail(
      ApplicationDetail applicationDetail, ExecutionLogCallback executionLogCallback) {
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

  public File downloadArtifact(List<ArtifactFile> artifactFiles, String accountId, File workingDirecotry)
      throws IOException, ExecutionException {
    List<Pair<String, String>> fileIds = Lists.newArrayList();

    if (isEmpty(artifactFiles)) {
      throw new InvalidArgumentsException(Pair.of("Artifact", "is not available"));
    }

    artifactFiles.forEach(artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));
    try (InputStream inputStream =
             delegateFileManager.downloadArtifactByFileId(FileBucket.ARTIFACTS, fileIds.get(0).getKey(), accountId)) {
      String fileName = System.currentTimeMillis() + artifactFiles.get(0).getName();
      File artifactFile = new File(workingDirecotry.getAbsolutePath() + "/" + fileName);

      if (!artifactFile.createNewFile()) {
        throw new WingsException(ErrorCode.GENERAL_ERROR)
            .addParam("message", "Failed to create file " + artifactFile.getCanonicalPath());
      }
      IOUtils.copy(inputStream, new FileOutputStream(artifactFile));
      return artifactFile;
    }
  }

  @VisibleForTesting
  ApplicationDetail downSize(PcfServiceData pcfServiceData, ExecutionLogCallback executionLogCallback,
      PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager) throws PivotalClientApiException {
    pcfRequestConfig.setApplicationName(pcfServiceData.getName());
    pcfRequestConfig.setDesiredCount(pcfServiceData.getDesiredCount());

    ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(pcfRequestConfig);

    executionLogCallback.saveExecutionLog("# Downsizing successful");
    executionLogCallback.saveExecutionLog("\n# App details after downsize:");
    printApplicationDetail(applicationDetail, executionLogCallback);
    return applicationDetail;
  }

  public void deleteCreatedFile(List<File> files) {
    files.forEach(File::delete);
  }

  public String generateFinalManifestFilePath(String path) {
    return path.replace(".yml", "_1.yml");
  }

  public String getAppPrefix(String appName) {
    return appName.substring(0, appName.lastIndexOf(DELIMITER));
  }

  public int getRevisionFromReleaseName(String name) {
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

  public File createManifestYamlFileLocally(PcfCreateApplicationRequestData requestData) throws IOException {
    File manifestFile = getManifestFile(requestData);
    return writeToManifestFile(requestData.getFinalManifestYaml(), manifestFile);
  }

  public File createYamlFileLocally(String filePath, String content) throws IOException {
    File file = new File(filePath);
    return writeToManifestFile(content, file);
  }

  public File createManifestVarsYamlFileLocally(
      PcfCreateApplicationRequestData requestData, String varsContent, int index) {
    try {
      if (isBlank(varsContent)) {
        return null;
      }

      File manifestFile = getManifestVarsFile(requestData, index);
      return writeToManifestFile(varsContent, manifestFile);
    } catch (IOException e) {
      throw new UnexpectedException("Failed while writting manifest file on disk", e);
    }
  }

  @NotNull
  private File writeToManifestFile(String content, File manifestFile) throws IOException {
    if (!manifestFile.createNewFile()) {
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", "Failed to create file " + manifestFile.getCanonicalPath());
    }

    FileUtils.writeStringToFile(manifestFile, content, UTF_8);
    return manifestFile;
  }

  public File getManifestFile(PcfCreateApplicationRequestData requestData) {
    return new File(requestData.getConfigPathVar() + "/" + requestData.getNewReleaseName() + ".yml");
  }

  public File getManifestVarsFile(PcfCreateApplicationRequestData requestData, int index) {
    return new File(new StringBuilder(128)
                        .append(requestData.getConfigPathVar())
                        .append('/')
                        .append(requestData.getNewReleaseName())
                        .append("_vars_")
                        .append(index)
                        .append(".yml")
                        .toString());
  }

  /**
   * This is called from Deploy (Resize) phase.
   */
  public void upsizeNewApplication(ExecutionLogCallback executionLogCallback,
      PcfCommandDeployRequest pcfCommandDeployRequest, List<PcfServiceData> pcfServiceDataUpdated,
      PcfRequestConfig pcfRequestConfig, ApplicationDetail details, List<PcfInstanceElement> pcfInstanceElements)
      throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("# Upsizing new application:", White, Bold));

    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("APPLICATION-NAME: ")
                                              .append(details.getName())
                                              .append("\n" + CURRENT_INSTANCE_COUNT)
                                              .append(details.getInstances())
                                              .append("\n" + DESIRED_INSTANCE_COUNT)
                                              .append(pcfCommandDeployRequest.getUpdateCount())
                                              .toString());

    // Upscale new app
    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getNewReleaseName());
    pcfRequestConfig.setDesiredCount(pcfCommandDeployRequest.getUpdateCount());

    // perform upsize
    upsizeInstance(
        pcfRequestConfig, pcfDeploymentManager, executionLogCallback, pcfServiceDataUpdated, pcfInstanceElements);
  }

  private void upsizeInstance(PcfRequestConfig pcfRequestConfig, PcfDeploymentManager pcfDeploymentManager,
      ExecutionLogCallback executionLogCallback, List<PcfServiceData> pcfServiceDataUpdated,
      List<PcfInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
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
    ApplicationDetail detailsAfterUpsize =
        pcfDeploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, executionLogCallback);
    executionLogCallback.saveExecutionLog(sb.append("# Application upsized successfully ").toString());

    List<InstanceDetail> instances = detailsAfterUpsize.getInstanceDetails().stream().collect(toList());
    instances.forEach(instance
        -> pcfInstanceElements.add(PcfInstanceElement.builder()
                                       .applicationId(detailsAfterUpsize.getId())
                                       .displayName(detailsAfterUpsize.getName())
                                       .instanceIndex(instance.getIndex())
                                       .isUpsize(true)
                                       .build()));

    // Instance token is ApplicationGuid:InstanceIndex, that can be used to connect to instance from outside world
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("\n# Application state details after upsize:  ").toString());
    printApplicationDetail(detailsAfterUpsize, executionLogCallback);
    printInstanceDetails(executionLogCallback, instances);
  }

  public void mapRouteMaps(String applicationName, List<String> routes, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Adding Routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationName);
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(routes));
    // map
    pcfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.mapRouteMapForApplication(pcfRequestConfig, routes);
  }

  public void unmapExistingRouteMaps(ApplicationDetail applicationDetail, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Unmapping routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationDetail.getName());
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(applicationDetail.getUrls()));
    // map
    pcfRequestConfig.setApplicationName(applicationDetail.getName());
    pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, applicationDetail.getUrls());
  }

  public void unmapRouteMaps(String applicationName, List<String> routes, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Unmapping Routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationName);
    executionLogCallback.saveExecutionLog("ROUTES: \n[" + getRouteString(routes));
    // unmap
    pcfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, routes);
    executionLogCallback.saveExecutionLog("# Unmapping Routes was successfully completed");
  }

  public boolean disableAutoscalar(PcfAppAutoscalarRequestData pcfAppAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    return pcfDeploymentManager.changeAutoscalarState(pcfAppAutoscalarRequestData, executionLogCallback, false);
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

  public String generateManifestYamlForPush(PcfCreateApplicationRequestData requestData)
      throws PivotalClientApiException {
    // Substitute name,
    String manifestYaml = requestData.getSetupRequest().getManifestYaml();

    Map<String, Object> map = (Map<String, Object>) yaml.load(manifestYaml);
    List<Map> applicationMaps = (List<Map>) map.get(APPLICATION_YML_ELEMENT);

    if (isEmpty(applicationMaps)) {
      throw new InvalidArgumentsException(
          Pair.of("Manifest.yml does not have any elements under \'applications\'", manifestYaml));
    }

    // We always assume, first app is main application being deployed.
    Map mapForUpdate = applicationMaps.get(0);

    // Use TreeMap that uses case insensitive keys
    TreeMap<String, Object> applicationToBeUpdated = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    applicationToBeUpdated.putAll(mapForUpdate);

    // Update Name only if vars file is not present, legacy
    applicationToBeUpdated.put(NAME_MANIFEST_YML_ELEMENT, requestData.getNewReleaseName());
    applicationToBeUpdated.put(PATH_MANIFEST_YML_ELEMENT, requestData.getArtifactPath());
    applicationToBeUpdated.put(INSTANCE_MANIFEST_YML_ELEMENT, 0);

    // Update routes.
    updateConfigWithRoutesIfRequired(requestData, applicationToBeUpdated);
    // We do not want to change order

    // remove "create-services" elements as it would have been used by cf cli plugin to create services.
    // This elements is not needed for cf push
    map.remove(CREATE_SERVICE_MANIFEST_ELEMENT);
    Map<String, Object> applicationMapForYamlDump = generateFinalMapForYamlDump(applicationToBeUpdated);

    // replace map for first application that we are deploying
    applicationMaps.set(0, applicationMapForYamlDump);
    try {
      return yaml.dump(map);
    } catch (Exception e) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Failed to generate final version of  Manifest.yml file. ")
                                              .append(manifestYaml)
                                              .toString(),
          e);
    }
  }

  public File generateWorkingDirectoryForDeployment(String workingDirecotry) throws IOException {
    FileIo.createDirectoryIfDoesNotExist(REPOSITORY_DIR_PATH);
    FileIo.createDirectoryIfDoesNotExist(PCF_ARTIFACT_DOWNLOAD_DIR_PATH);
    String workingDir = PCF_ARTIFACT_DOWNLOAD_DIR_PATH + "/" + workingDirecotry;
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    return new File(workingDir);
  }

  private Map<String, Object> generateFinalMapForYamlDump(TreeMap<String, Object> applicationToBeUpdated) {
    Map<String, Object> yamlMap = new LinkedHashMap<>();

    addToMapIfExists(yamlMap, applicationToBeUpdated, NAME_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, MEMORY_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, INSTANCE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, BUILDPACK_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, BUILDPACKS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, PATH_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, COMMAND_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DISK_QUOTA_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DOCKER_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DOMAINS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ENV_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HEALTH_CHECK_HTTP_ENDPOINT_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HEALTH_CHECK_TYPE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HOSTS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HOST_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, NO_HOSTNAME_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, NO_ROUTE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, RANDOM_ROUTE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ROUTE_PATH_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ROUTES_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, SERVICES_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, STACK_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, TIMEOUT_MANIFEST_YML_ELEMENT);

    return yamlMap;
  }

  private void addToMapIfExists(Map destMap, Map sourceMap, String element) {
    if (sourceMap.containsKey(element)) {
      destMap.put(element, sourceMap.get(element));
    }
  }

  private void updateConfigWithRoutesIfRequired(
      PcfCreateApplicationRequestData requestData, TreeMap applicationToBeUpdated) {
    PcfCommandSetupRequest setupRequest = requestData.getSetupRequest();

    applicationToBeUpdated.remove(ROUTES_MANIFEST_YML_ELEMENT);

    // 1. Check and handle no-route scenario
    boolean isNoRoute = applicationToBeUpdated.containsKey(NO_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) applicationToBeUpdated.get(NO_ROUTE_MANIFEST_YML_ELEMENT);
    if (isNoRoute) {
      handleManifestWithNoRoute(applicationToBeUpdated, setupRequest.isBlueGreen());
      return;
    }

    // 2. Check if random-route config is needed. This happens if random-route=true in manifest or
    // user has not provided any route value.
    if (shouldUseRandomRoute(applicationToBeUpdated, setupRequest)) {
      applicationToBeUpdated.put(RANDOM_ROUTE_MANIFEST_YML_ELEMENT, true);
      return;
    }

    // 3. Insert routes provided by user.
    List<String> routesForUse = setupRequest.getRouteMaps();
    List<Map<String, String>> routeMapList = new ArrayList<>();
    routesForUse.forEach(routeString -> {
      Map<String, String> mapEntry = Collections.singletonMap(ROUTE_MANIFEST_YML_ELEMENT, routeString);
      routeMapList.add(mapEntry);
    });

    // Add this route config to applicationConfig
    applicationToBeUpdated.put(ROUTES_MANIFEST_YML_ELEMENT, routeMapList);
  }

  private boolean shouldUseRandomRoute(Map applicationToBeUpdated, PcfCommandSetupRequest setupRequest) {
    return manifestContainsRandomRouteElement(applicationToBeUpdated) || isEmpty(setupRequest.getRouteMaps());
  }

  private boolean manifestContainsRandomRouteElement(Map applicationToBeUpdated) {
    return applicationToBeUpdated.containsKey(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) applicationToBeUpdated.get(RANDOM_ROUTE_MANIFEST_YML_ELEMENT);
  }

  @VisibleForTesting
  void handleManifestWithNoRoute(Map applicationToBeUpdated, boolean isBlueGreen) {
    // No route is not allowed for BG
    if (isBlueGreen) {
      throw new InvalidRequestException("Invalid Config. \"no-route\" can not be used with BG deployment");
    }

    // If no-route = true, then route element is not needed.
    applicationToBeUpdated.remove(ROUTES_MANIFEST_YML_ELEMENT);
  }

  @VisibleForTesting
  void handleRandomRouteScenario(PcfCreateApplicationRequestData requestData, Map applicationToBeUpdated) {
    applicationToBeUpdated.put(RANDOM_ROUTE_MANIFEST_YML_ELEMENT, true);
    if (!applicationToBeUpdated.containsKey(HOST_MANIFEST_YML_ELEMENT)) {
      // Random-routes needs to be generated.  ransom-route uses host mentioned in manifest to generate route.
      // If that is not present, generate some host name contextual to current deployment.
      // We add space to this hostName, as we want to generate routes space specific, as same route can not be in
      // multiple spaces.
      String appName = requestData.getNewReleaseName();
      String appPrefix = appName.substring(0, appName.lastIndexOf("__"));
      appPrefix = appPrefix + '-' + requestData.getPcfRequestConfig().getSpaceName();
      // '_' in routemap is not allowed, PCF lets us create route but while accessing it, fails
      appPrefix = appPrefix.replace("__", "-");
      appPrefix = appPrefix.replace("_", "-");

      applicationToBeUpdated.put(HOST_MANIFEST_YML_ELEMENT, appPrefix);
    }
  }

  public void printFileNamesInExecutionLogs(List<String> filePathList, ExecutionLogCallback executionLogCallback) {
    if (EmptyPredicate.isEmpty(filePathList)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    filePathList.forEach(filePath -> sb.append(color(format("- %s", filePath), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(sb.toString());
  }
}
