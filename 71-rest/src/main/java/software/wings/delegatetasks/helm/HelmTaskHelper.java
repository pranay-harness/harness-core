package software.wings.delegatetasks.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_URL;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_PATH_PLACEHOLDER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.FileData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumServer;
import software.wings.helpers.ext.helm.HelmCommandTemplateFactory;
import software.wings.helpers.ext.helm.HelmCommandTemplateFactory.HelmCliCommandType;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class HelmTaskHelper {
  private static final String WORKING_DIR_BASE = "./repository/helm-values/";
  private static final String RESOURCE_DIR_BASE = "./repository/helm/resources/";
  private static final String VALUES_YAML = "values.yaml";

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private EncryptionService encryptionService;
  @Inject private ChartMuseumClient chartMuseumClient;

  private void fetchChartFiles(HelmChartConfigParams helmChartConfigParams, String destinationDirectory)
      throws Exception {
    HelmRepoConfig helmRepoConfig = helmChartConfigParams.getHelmRepoConfig();

    initHelm(destinationDirectory, helmChartConfigParams.getHelmVersion());

    if (helmRepoConfig == null) {
      fetchChartFromEmptyHelmRepoConfig(helmChartConfigParams, destinationDirectory);
    } else {
      encryptionService.decrypt(helmRepoConfig, helmChartConfigParams.getEncryptedDataDetails());

      SettingValue connectorConfig = helmChartConfigParams.getConnectorConfig();
      if (connectorConfig != null) {
        encryptionService.decrypt(
            (EncryptableSetting) connectorConfig, helmChartConfigParams.getConnectorEncryptedDataDetails());
      }

      if (helmRepoConfig instanceof AmazonS3HelmRepoConfig || helmRepoConfig instanceof GCSHelmRepoConfig) {
        fetchChartUsingChartMuseumServer(helmChartConfigParams, connectorConfig, destinationDirectory);
      } else if (helmRepoConfig instanceof HttpHelmRepoConfig) {
        fetchChartFromHttpServer(helmChartConfigParams, destinationDirectory);
      }
    }
  }

  public void downloadChartFiles(HelmChartConfigParams helmChartConfigParams, String destinationDirectory)
      throws Exception {
    String workingDirectory = createDirectory(Paths.get(destinationDirectory).toString());

    fetchChartFiles(helmChartConfigParams, workingDirectory);
  }

  public String getValuesYamlFromChart(HelmChartConfigParams helmChartConfigParams) throws Exception {
    String workingDirectory = createNewDirectoryAtPath(Paths.get(WORKING_DIR_BASE).toString());

    try {
      fetchChartFiles(helmChartConfigParams, workingDirectory);

      return new String(
          Files.readAllBytes(Paths.get(workingDirectory, helmChartConfigParams.getChartName(), VALUES_YAML)),
          StandardCharsets.UTF_8);
    } catch (Exception ex) {
      logger.info("values.yaml file not found", ex);
      return null;
    } finally {
      cleanup(workingDirectory);
    }
  }

  private void fetchChartUsingChartMuseumServer(HelmChartConfigParams helmChartConfigParams,
      SettingValue connectorConfig, String chartDirectory) throws Exception {
    ChartMuseumServer chartMuseumServer = null;
    String resourceDirectory = null;

    try {
      resourceDirectory = createNewDirectoryAtPath(RESOURCE_DIR_BASE);
      chartMuseumServer = chartMuseumClient.startChartMuseumServer(helmChartConfigParams.getHelmRepoConfig(),
          connectorConfig, resourceDirectory, helmChartConfigParams.getBasePath());

      addChartMuseumRepo(helmChartConfigParams.getRepoName(), helmChartConfigParams.getRepoDisplayName(),
          chartMuseumServer.getPort(), chartDirectory, helmChartConfigParams.getHelmVersion());
      fetchChartFromRepo(helmChartConfigParams, chartDirectory);
    } finally {
      if (chartMuseumServer != null) {
        chartMuseumClient.stopChartMuseumServer(chartMuseumServer.getStartedProcess());
      }
      removeRepo(helmChartConfigParams.getRepoName(), chartDirectory, helmChartConfigParams.getHelmVersion());
      cleanup(resourceDirectory);
    }
  }

  private String getHelmHomePath(String workingDirectory) {
    return Paths.get(workingDirectory, "helm").normalize().toAbsolutePath().toString();
  }

  private String applyHelmHomePath(String command, String workingDirectory) {
    if (isBlank(workingDirectory)) {
      return command.replace("${HELM_HOME_PATH_FLAG}", "");
    } else {
      String helmHomePath = getHelmHomePath(workingDirectory);
      return command.replace("${HELM_HOME_PATH_FLAG}", "--home " + helmHomePath);
    }
  }

  public void initHelm(String workingDirectory, HelmVersion helmVersion) throws Exception {
    String helmHomePath = getHelmHomePath(workingDirectory);
    createNewDirectoryAtPath(helmHomePath);

    // Helm init command would be blank for helmV3
    String helmInitCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.INIT, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion)));
    if (isNotBlank(helmHomePath) && isNotBlank(helmInitCommand)) {
      helmInitCommand = applyHelmHomePath(helmInitCommand, workingDirectory);
      logger.info("Initing helm. Command " + helmInitCommand);

      ProcessResult processResult = executeCommand(helmInitCommand, workingDirectory);
      if (processResult.getExitValue() != 0) {
        throw new WingsException(
            "Failed to init helm. Executed command " + helmInitCommand + ". " + processResult.getOutput().getUTF8(),
            USER);
      }
    }
  }

  private void addChartMuseumRepo(String repoName, String repoDisplayName, int port, String chartDirectory,
      HelmVersion helmVersion) throws Exception {
    String repoAddCommand = getChartMuseumRepoAddCommand(repoName, port, chartDirectory, helmVersion);
    logger.info(repoAddCommand);
    logger.info("helm repo add command for repository " + repoDisplayName);

    ProcessResult processResult = executeCommand(repoAddCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      throw new WingsException(
          "Failed to add helm repo. Executed command " + repoAddCommand + ". " + processResult.getOutput().getUTF8(),
          USER);
    }
  }

  private void executeFetchChartFromRepo(
      HelmChartConfigParams helmChartConfigParams, String chartDirectory, String helmFetchCommand) throws Exception {
    logger.info(helmFetchCommand);

    ProcessResult processResult = executeCommand(helmFetchCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      StringBuilder builder = new StringBuilder()
                                  .append("Failed to fetch chart \"")
                                  .append(helmChartConfigParams.getChartName())
                                  .append("\" ");

      if (isNotBlank(helmChartConfigParams.getRepoDisplayName())) {
        builder.append(" from repo \"").append(helmChartConfigParams.getRepoDisplayName()).append("\". ");
      }
      builder.append("Please check if the chart is present in the repo.");

      throw new InvalidRequestException(builder.toString(), USER);
    }
  }

  private void fetchChartFromRepo(HelmChartConfigParams helmChartConfigParams, String chartDirectory) throws Exception {
    String helmFetchCommand =
        getHelmFetchCommand(helmChartConfigParams.getChartName(), helmChartConfigParams.getChartVersion(),
            helmChartConfigParams.getRepoName(), chartDirectory, helmChartConfigParams.getHelmVersion());
    executeFetchChartFromRepo(helmChartConfigParams, chartDirectory, helmFetchCommand);
  }

  private String getHelmFetchCommand(
      String chartName, String chartVersion, String repoName, String workingDirectory, HelmVersion helmVersion) {
    String helmFetchCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.FETCH, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion)))
            .replace("${CHART_NAME}", chartName)
            .replace("${CHART_VERSION}", getChartVersion(chartVersion));

    if (isNotBlank(repoName)) {
      helmFetchCommand = helmFetchCommand.replace("${REPO_NAME}", repoName);
    } else {
      helmFetchCommand = helmFetchCommand.replace("${REPO_NAME}/", "");
    }

    return applyHelmHomePath(helmFetchCommand, workingDirectory);
  }

  private String getChartMuseumRepoAddCommand(
      String repoName, int port, String workingDirectory, HelmVersion helmVersion) {
    String repoUrl = CHART_MUSEUM_SERVER_URL.replace("${PORT}", Integer.toString(port));

    String repoAddCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_ADD_CHART_MEUSEUM, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion)))
            .replace("${REPO_NAME}", repoName)
            .replace("${REPO_URL}", repoUrl);

    return applyHelmHomePath(repoAddCommand, workingDirectory);
  }

  private ProcessResult executeCommand(String command, String directoryPath) throws Exception {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(isNotBlank(directoryPath) ? new File(directoryPath) : null)
                                          .timeout(2, TimeUnit.MINUTES)
                                          .commandSplit(command)
                                          .readOutput(true);

    return processExecutor.execute();
  }

  public String createNewDirectoryAtPath(String directoryBase) throws Exception {
    String workingDirectory = Paths.get(directoryBase, convertBase64UuidToCanonicalForm(generateUuid()))
                                  .normalize()
                                  .toAbsolutePath()
                                  .toString();

    createDirectoryIfDoesNotExist(workingDirectory);
    waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);

    return workingDirectory;
  }

  public String createDirectory(String directoryBase) throws Exception {
    String workingDirectory = Paths.get(directoryBase).normalize().toAbsolutePath().toString();

    createDirectoryIfDoesNotExist(workingDirectory);
    waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);

    return workingDirectory;
  }

  private String getChartVersion(String chartVersion) {
    return isBlank(chartVersion) ? StringUtils.EMPTY : "--version " + chartVersion;
  }

  private List<FileData> getFiles(String chartDirectory, String chartName) {
    Path chartPath = Paths.get(chartDirectory, chartName);

    try {
      return getFilesUnderPath(chartPath.toString());
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      throw new WingsException("Failed to get files. Error: " + ExceptionUtils.getMessage(ex));
    }
  }

  public List<FileData> getFilteredFiles(List<FileData> files, List<String> filesToBeFetched) {
    List<FileData> filteredFiles = new ArrayList<>();

    if (isEmpty(files)) {
      logger.info("Files list is empty");
      return filteredFiles;
    }

    Set<String> filesToBeFetchedSet = new HashSet<>(filesToBeFetched);
    for (FileData file : files) {
      if (filesToBeFetchedSet.contains(file.getFilePath())) {
        filteredFiles.add(file);
      }
    }

    return filteredFiles;
  }

  public void cleanup(String workingDirectory) {
    try {
      logger.warn("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      logger.warn("Exception in directory cleanup.", ex);
    }
  }

  public void printHelmChartInfoInExecutionLogs(
      HelmChartConfigParams helmChartConfigParams, ExecutionLogCallback executionLogCallback) {
    if (isNotBlank(helmChartConfigParams.getRepoDisplayName())) {
      executionLogCallback.saveExecutionLog("Helm repository: " + helmChartConfigParams.getRepoDisplayName());
    }

    if (isNotBlank(helmChartConfigParams.getBasePath())) {
      executionLogCallback.saveExecutionLog("Base Path: " + helmChartConfigParams.getBasePath());
    }

    if (isNotBlank(helmChartConfigParams.getChartName())) {
      executionLogCallback.saveExecutionLog("Chart name: " + helmChartConfigParams.getChartName());
    }

    if (isNotBlank(helmChartConfigParams.getChartVersion())) {
      executionLogCallback.saveExecutionLog("Chart version: " + helmChartConfigParams.getChartVersion());
    }

    if (isNotBlank(helmChartConfigParams.getChartUrl())) {
      executionLogCallback.saveExecutionLog("Chart url: " + helmChartConfigParams.getChartUrl());
    }

    if (helmChartConfigParams.getHelmVersion() != null) {
      executionLogCallback.saveExecutionLog("Helm version: " + helmChartConfigParams.getHelmVersion());
    }

    if (helmChartConfigParams.getHelmRepoConfig() instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig amazonS3HelmRepoConfig =
          (AmazonS3HelmRepoConfig) helmChartConfigParams.getHelmRepoConfig();
      executionLogCallback.saveExecutionLog("Chart bucket: " + amazonS3HelmRepoConfig.getBucketName());
    } else if (helmChartConfigParams.getHelmRepoConfig() instanceof HttpHelmRepoConfig) {
      executionLogCallback.saveExecutionLog(
          "Repo url: " + ((HttpHelmRepoConfig) helmChartConfigParams.getHelmRepoConfig()).getChartRepoUrl());
    }
  }

  private String getUsername(String username) {
    return isBlank(username) ? StringUtils.EMPTY : "--username " + username;
  }

  private String getPassword(char[] password) {
    return isEmpty(password) ? StringUtils.EMPTY : "--password " + new String(password);
  }

  private String getHttpRepoAddCommand(String repoName, String chartRepoUrl, String username, char[] password,
      String workingDirectory, HelmVersion helmVersion) {
    String addRepoCommand =
        getHttpRepoAddCommandWithoutPassword(repoName, chartRepoUrl, username, workingDirectory, helmVersion);

    return addRepoCommand.replace("${PASSWORD}", getPassword(password));
  }

  public void addRepo(String repoName, String repoDisplayName, String chartRepoUrl, String username, char[] password,
      String chartDirectory, HelmVersion helmVersion) throws Exception {
    String repoAddCommand =
        getHttpRepoAddCommand(repoName, chartRepoUrl, username, password, chartDirectory, helmVersion);

    String repoAddCommandForLogging =
        getHttpRepoAddCommandForLogging(repoName, chartRepoUrl, username, password, chartDirectory, helmVersion);
    logger.info(repoAddCommandForLogging);
    logger.info("helm repo add command for repository " + repoDisplayName);

    ProcessResult processResult = executeCommand(repoAddCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      throw new WingsException("Failed to add helm repo. Executed command " + repoAddCommandForLogging + ". "
              + processResult.getOutput().getUTF8(),
          USER);
    }
  }

  private void fetchChartFromHttpServer(HelmChartConfigParams helmChartConfigParams, String chartDirectory)
      throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmChartConfigParams.getHelmRepoConfig();

    addRepo(helmChartConfigParams.getRepoName(), helmChartConfigParams.getRepoDisplayName(),
        httpHelmRepoConfig.getChartRepoUrl(), httpHelmRepoConfig.getUsername(), httpHelmRepoConfig.getPassword(),
        chartDirectory, helmChartConfigParams.getHelmVersion());
    fetchChartFromRepo(helmChartConfigParams, chartDirectory);
  }

  private String getHttpRepoAddCommandForLogging(String repoName, String chartRepoUrl, String username, char[] password,
      String workingDirectory, HelmVersion helmVersion) {
    String repoAddCommand =
        getHttpRepoAddCommandWithoutPassword(repoName, chartRepoUrl, username, workingDirectory, helmVersion);
    String evaluatedPassword = isEmpty(password) ? StringUtils.EMPTY : "--password *******";

    return repoAddCommand.replace("${PASSWORD}", evaluatedPassword);
  }

  private String getHttpRepoAddCommandWithoutPassword(
      String repoName, String chartRepoUrl, String username, String workingDirectory, HelmVersion helmVersion) {
    String command =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_ADD_HTTP, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion)))
            .replace("${REPO_NAME}", repoName)
            .replace("${REPO_URL}", chartRepoUrl)
            .replace("${USERNAME}", getUsername(username));

    return applyHelmHomePath(command, workingDirectory);
  }

  public void addHelmRepo(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig, String repoName,
      String repoDisplayName, String workingDirectory, String basePath, HelmVersion helmVersion) throws Exception {
    ChartMuseumServer chartMuseumServer = null;
    String resourceDirectory = null;
    try {
      resourceDirectory = createNewDirectoryAtPath(RESOURCE_DIR_BASE);
      chartMuseumServer =
          chartMuseumClient.startChartMuseumServer(helmRepoConfig, connectorConfig, resourceDirectory, basePath);

      addChartMuseumRepo(repoName, repoDisplayName, chartMuseumServer.getPort(), workingDirectory, helmVersion);
    } finally {
      if (chartMuseumServer != null) {
        chartMuseumClient.stopChartMuseumServer(chartMuseumServer.getStartedProcess());
      }
      cleanup(resourceDirectory);
    }
  }

  private String getRepoRemoveCommand(String repoName, String workingDirectory, HelmVersion helmVersion) {
    String repoRemoveCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_REMOVE, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion)))
            .replace("${REPO_NAME}", repoName);

    return applyHelmHomePath(repoRemoveCommand, workingDirectory);
  }

  public void removeRepo(String repoName, String workingDirectory, HelmVersion helmVersion) {
    try {
      String repoRemoveCommand = getRepoRemoveCommand(repoName, workingDirectory, helmVersion);

      ProcessResult processResult = executeCommand(repoRemoveCommand, null);
      if (processResult.getExitValue() != 0) {
        logger.warn("Failed to remove helm repo {}. {}", repoName, processResult.getOutput().getUTF8());
      }
    } catch (Exception ex) {
      logger.warn(ExceptionUtils.getMessage(ex));
    }
  }

  /*
  This method is called in case the helm has empty repository connector and the chartName has <REPO_NAME/CHART_NAME>
  value. In that case, we want to use the default "$HELM_HOME" path. That is why :-
  1.) We are not adding repo if the URL is empty
  2.) Passing null directoryPath in the helmFetchCommand so that it picks up default helm
  Ruckus is one of the customer that is using this mechanism
   */
  private void fetchChartFromEmptyHelmRepoConfig(HelmChartConfigParams helmChartConfigParams, String chartDirectory)
      throws Exception {
    try {
      String helmFetchCommand;
      if (isNotBlank(helmChartConfigParams.getChartUrl())) {
        addRepo(helmChartConfigParams.getRepoName(), null, helmChartConfigParams.getChartUrl(), null, null,
            chartDirectory, helmChartConfigParams.getHelmVersion());
        helmFetchCommand =
            getHelmFetchCommand(helmChartConfigParams.getChartName(), helmChartConfigParams.getChartVersion(),
                helmChartConfigParams.getRepoName(), chartDirectory, helmChartConfigParams.getHelmVersion());
      } else {
        helmFetchCommand =
            getHelmFetchCommand(helmChartConfigParams.getChartName(), helmChartConfigParams.getChartVersion(),
                helmChartConfigParams.getRepoName(), null, helmChartConfigParams.getHelmVersion());
      }
      executeFetchChartFromRepo(helmChartConfigParams, chartDirectory, helmFetchCommand);

    } finally {
      if (isNotBlank(helmChartConfigParams.getChartUrl())) {
        removeRepo(helmChartConfigParams.getRepoName(), chartDirectory, helmChartConfigParams.getHelmVersion());
      }
    }
  }
}
