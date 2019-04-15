package software.wings.delegatetasks.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_URL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.FileData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumServer;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
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

  private static final String HELM_REPO_ADD_COMMAND_FOR_CHART_MUSEUM = "${HELM_PATH} repo add ${REPO_NAME} ${REPO_URL}";
  private static final String HELM_REPO_ADD_COMMAND_FOR_HTTP =
      "${HELM_PATH} repo add ${REPO_NAME} ${REPO_URL} ${USERNAME} ${PASSWORD}";

  private static final String HELM_FETCH_COMMAND =
      "${HELM_PATH} fetch ${REPO_NAME}/${CHART_NAME} --untar ${CHART_VERSION}";

  private static final String HELM_REPO_REMOVE_COMMAND = "${HELM_PATH} repo remove ${REPO_NAME}";

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private EncryptionService encryptionService;
  @Inject private ChartMuseumClient chartMuseumClient;

  public List<FileData> fetchChartFiles(HelmChartConfigParams helmChartConfigParams, List<String> filesToBeFetched)
      throws Exception {
    HelmRepoConfig helmRepoConfig = helmChartConfigParams.getHelmRepoConfig();
    encryptionService.decrypt(helmRepoConfig, helmChartConfigParams.getEncryptedDataDetails());

    SettingValue connectorConfig = helmChartConfigParams.getConnectorConfig();
    if (connectorConfig != null) {
      encryptionService.decrypt(
          (EncryptableSetting) connectorConfig, helmChartConfigParams.getConnectorEncryptedDataDetails());
    }

    String workingDirectory = null;
    try {
      workingDirectory = createWorkingDirectory();

      if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
        fetchChartUsingChartMuseumServer(helmChartConfigParams, connectorConfig, workingDirectory);
      } else if (helmRepoConfig instanceof HttpHelmRepoConfig) {
        fetchChartFromHttpServer(helmChartConfigParams, workingDirectory);
      }

      List<FileData> files = getFiles(workingDirectory, helmChartConfigParams.getChartName());
      if (isEmpty(filesToBeFetched)) {
        return files;
      }

      return getFilteredFiles(files, filesToBeFetched);
    } finally {
      cleanup(workingDirectory);
    }
  }

  private void fetchChartUsingChartMuseumServer(HelmChartConfigParams helmChartConfigParams,
      SettingValue connectorConfig, String chartDirectory) throws Exception {
    ChartMuseumServer chartMuseumServer = null;

    try {
      chartMuseumServer =
          chartMuseumClient.startChartMuseumServer(helmChartConfigParams.getHelmRepoConfig(), connectorConfig);

      addChartMuseumRepo(helmChartConfigParams.getRepoName(), helmChartConfigParams.getRepoDisplayName(),
          chartMuseumServer.getPort(), chartDirectory);
      fetchChartFromRepo(helmChartConfigParams, chartDirectory);
    } finally {
      if (chartMuseumServer != null) {
        chartMuseumClient.stopChartMuseumServer(chartMuseumServer.getStartedProcess());
      }
    }
  }

  private void addChartMuseumRepo(String repoName, String repoDisplayName, int port, String chartDirectory)
      throws Exception {
    String repoAddCommand = getChartMuseumRepoAddCommand(repoName, port);
    logger.info(repoAddCommand);
    logger.info("helm repo add command for repository " + repoDisplayName);

    ProcessResult processResult = executeCommand(repoAddCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      throw new WingsException("Failed to add helm repo. " + processResult.getOutput().getUTF8());
    }
  }

  private void fetchChartFromRepo(HelmChartConfigParams helmChartConfigParams, String chartDirectory) throws Exception {
    String helmFetchCommand = getHelmFetchCommand(helmChartConfigParams.getChartName(),
        helmChartConfigParams.getChartVersion(), helmChartConfigParams.getRepoName());
    logger.info(helmFetchCommand);

    ProcessResult processResult = executeCommand(helmFetchCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      throw new WingsException(
          format("Failed to fetch chart %s from repo %s. Error: %s", helmChartConfigParams.getChartName(),
              helmChartConfigParams.getRepoDisplayName(), processResult.getOutput().getUTF8()));
    }
  }

  private String getHelmFetchCommand(String chartName, String chartVersion, String repoName) {
    return HELM_FETCH_COMMAND.replace("${HELM_PATH}", encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
        .replace("${REPO_NAME}", repoName)
        .replace("${CHART_NAME}", chartName)
        .replace("${CHART_VERSION}", getChartVersion(chartVersion));
  }

  private String getChartMuseumRepoAddCommand(String repoName, int port) {
    String repoUrl = CHART_MUSEUM_SERVER_URL.replace("${PORT}", Integer.toString(port));

    return HELM_REPO_ADD_COMMAND_FOR_CHART_MUSEUM
        .replace("${HELM_PATH}", encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
        .replace("${REPO_NAME}", repoName)
        .replace("${REPO_URL}", repoUrl);
  }

  private ProcessResult executeCommand(String command, String directoryPath) throws Exception {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(isNotBlank(directoryPath) ? new File(directoryPath) : null)
                                          .timeout(2, TimeUnit.MINUTES)
                                          .commandSplit(command)
                                          .readOutput(true);

    return processExecutor.execute();
  }

  public String createWorkingDirectory() throws Exception {
    String workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                                  .normalize()
                                  .toAbsolutePath()
                                  .toString();

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

  private void cleanup(String workingDirectory) {
    try {
      logger.warn("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      logger.warn("Exception in directory cleanup.", ex);
    }
  }

  public void printHelmChartInfoInExecutionLogs(
      HelmChartConfigParams helmChartConfigParams, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Helm repository: " + helmChartConfigParams.getRepoDisplayName());
    executionLogCallback.saveExecutionLog("Chart name: " + helmChartConfigParams.getChartName());
    executionLogCallback.saveExecutionLog("Chart version: " + helmChartConfigParams.getChartVersion());

    if (helmChartConfigParams.getHelmRepoConfig() instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig amazonS3HelmRepoConfig =
          (AmazonS3HelmRepoConfig) helmChartConfigParams.getHelmRepoConfig();
      executionLogCallback.saveExecutionLog("Chart bucket: " + amazonS3HelmRepoConfig.getBucketName());
    } else if (helmChartConfigParams.getHelmRepoConfig() instanceof HttpHelmRepoConfig) {
      executionLogCallback.saveExecutionLog(
          "Repo url: " + ((HttpHelmRepoConfig) helmChartConfigParams.getHelmRepoConfig()).getChartRepoUrl());
    }
  }

  private String getUsername(HelmRepoConfig helmRepoConfig) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    return isBlank(httpHelmRepoConfig.getUsername()) ? StringUtils.EMPTY
                                                     : "--username " + httpHelmRepoConfig.getUsername();
  }

  private String getPassword(HelmRepoConfig helmRepoConfig) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    return isEmpty(httpHelmRepoConfig.getPassword()) ? StringUtils.EMPTY
                                                     : "--password " + new String(httpHelmRepoConfig.getPassword());
  }

  private String getHttpRepoAddCommand(HelmRepoConfig helmRepoConfig, String repoName, String repoDisplayName) {
    String addRepoCommand =
        HELM_REPO_ADD_COMMAND_FOR_HTTP
            .replace("${HELM_PATH}", encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
            .replace("${REPO_NAME}", repoName)
            .replace("${REPO_URL}", ((HttpHelmRepoConfig) helmRepoConfig).getChartRepoUrl());

    String userName = getUsername(helmRepoConfig);
    String password = getPassword(helmRepoConfig);

    printRepoAddCommand(addRepoCommand, userName, password, repoDisplayName);
    return addRepoCommand.replace("${USERNAME}", userName).replace("${PASSWORD}", password);
  }

  public void addHttpRepo(HelmRepoConfig helmRepoConfig, String repoName, String repoDisplayName, String chartDirectory)
      throws Exception {
    String repoAddCommand = getHttpRepoAddCommand(helmRepoConfig, repoName, repoDisplayName);

    ProcessResult processResult = executeCommand(repoAddCommand, chartDirectory);
    if (processResult.getExitValue() != 0) {
      throw new WingsException("Failed to add helm repo. " + processResult.getOutput().getUTF8());
    }
  }

  private void fetchChartFromHttpServer(HelmChartConfigParams helmChartConfigParams, String chartDirectory)
      throws Exception {
    addHttpRepo(helmChartConfigParams.getHelmRepoConfig(), helmChartConfigParams.getRepoName(),
        helmChartConfigParams.getRepoDisplayName(), chartDirectory);
    fetchChartFromRepo(helmChartConfigParams, chartDirectory);
  }

  private void printRepoAddCommand(String repoAddCommand, String username, String password, String repoDisplayName) {
    StringBuilder output = new StringBuilder(repoAddCommand.replace("${USERNAME}", "").replace("${PASSWORD}", ""));

    if (isNotBlank(username)) {
      output.append(" --username *******");
    }
    if (isNotBlank(password)) {
      output.append(" --password *******");
    }

    logger.info(output.toString());
    logger.info("helm repo add command for repository " + repoDisplayName);
  }

  public void addAmazonS3HelmRepo(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig, String repoName,
      String repoDisplayName) throws Exception {
    ChartMuseumServer chartMuseumServer = null;

    try {
      chartMuseumServer = chartMuseumClient.startChartMuseumServer(helmRepoConfig, connectorConfig);

      addChartMuseumRepo(repoName, repoDisplayName, chartMuseumServer.getPort(), null);
    } finally {
      if (chartMuseumServer != null) {
        chartMuseumClient.stopChartMuseumServer(chartMuseumServer.getStartedProcess());
      }
    }
  }

  private String getRepoRemoveCommand(String repoName) {
    return HELM_REPO_REMOVE_COMMAND
        .replace("${HELM_PATH}", encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath()))
        .replace("${REPO_NAME}", repoName);
  }

  public void removeRepo(String repoName) throws Exception {
    String repoRemoveCommand = getRepoRemoveCommand(repoName);

    ProcessResult processResult = executeCommand(repoRemoveCommand, null);
    if (processResult.getExitValue() != 0) {
      throw new WingsException(
          format("Failed to remove helm repo %s. %s", repoName, processResult.getOutput().getUTF8()));
    }
  }
}
