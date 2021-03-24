package io.harness.delegate.task.helm;

import static io.harness.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_URL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.connector.helm.HttpHelmAuthType.USER_PASSWORD;
import static io.harness.delegate.beans.storeconfig.StoreDelegateConfigType.HTTP_HELM;
import static io.harness.delegate.beans.storeconfig.StoreDelegateConfigType.S3_HELM;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.helm.HelmConstants.ADD_COMMAND_FOR_REPOSITORY;
import static io.harness.helm.HelmConstants.HELM_HOME_PATH_FLAG;
import static io.harness.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static io.harness.helm.HelmConstants.PASSWORD;
import static io.harness.helm.HelmConstants.REPO_NAME;
import static io.harness.helm.HelmConstants.REPO_URL;
import static io.harness.helm.HelmConstants.USERNAME;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.chartmuseum.NGChartMuseumService;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HelmClientException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.helm.HelmCliCommandType;
import io.harness.helm.HelmCommandFlagsUtils;
import io.harness.helm.HelmCommandTemplateFactory;
import io.harness.helm.HelmSubCommandType;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Singleton
@Slf4j
public class HelmTaskHelperBase {
  public static final String RESOURCE_DIR_BASE = "./repository/helm/resources/";

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private NGChartMuseumService ngChartMuseumService;

  public void initHelm(String workingDirectory, HelmVersion helmVersion, long timeoutInMillis) throws IOException {
    String helmHomePath = getHelmHomePath(workingDirectory);
    createNewDirectoryAtPath(helmHomePath);

    // Helm init command would be blank for helmV3
    String helmInitCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.INIT, helmVersion)
                                 .replace(HELM_PATH_PLACEHOLDER, getHelmPath(helmVersion));
    if (isNotBlank(helmHomePath) && isNotBlank(helmInitCommand)) {
      helmInitCommand = applyHelmHomePath(helmInitCommand, workingDirectory);
      log.info("Initing helm. Command " + helmInitCommand);

      ProcessResult processResult =
          executeCommand(helmInitCommand, workingDirectory, "Initing helm Command " + helmInitCommand, timeoutInMillis);
      if (processResult.getExitValue() != 0) {
        throw new HelmClientException(
            "Failed to init helm. Executed command " + helmInitCommand + ". " + processResult.getOutput().getUTF8(),
            USER);
      }
    }
  }

  public void addRepo(String repoName, String repoDisplayName, String chartRepoUrl, String username, char[] password,
      String chartDirectory, HelmVersion helmVersion, long timeoutInMillis) {
    String repoAddCommand =
        getHttpRepoAddCommand(repoName, chartRepoUrl, username, password, chartDirectory, helmVersion);

    String repoAddCommandForLogging =
        getHttpRepoAddCommandForLogging(repoName, chartRepoUrl, username, password, chartDirectory, helmVersion);
    log.info(repoAddCommandForLogging);
    log.info(ADD_COMMAND_FOR_REPOSITORY + repoDisplayName);

    ProcessResult processResult = executeCommand(
        repoAddCommand, chartDirectory, "add helm repo. Executed command" + repoAddCommandForLogging, timeoutInMillis);
    if (processResult.getExitValue() != 0) {
      throw new HelmClientException("Failed to add helm repo. Executed command " + repoAddCommandForLogging + ". "
              + processResult.getOutput().getUTF8(),
          USER);
    }
  }

  private String getHttpRepoAddCommand(String repoName, String chartRepoUrl, String username, char[] password,
      String workingDirectory, HelmVersion helmVersion) {
    String addRepoCommand =
        getHttpRepoAddCommandWithoutPassword(repoName, chartRepoUrl, username, workingDirectory, helmVersion);

    return addRepoCommand.replace(PASSWORD, getPassword(password));
  }

  private String getHttpRepoAddCommandForLogging(String repoName, String chartRepoUrl, String username, char[] password,
      String workingDirectory, HelmVersion helmVersion) {
    String repoAddCommand =
        getHttpRepoAddCommandWithoutPassword(repoName, chartRepoUrl, username, workingDirectory, helmVersion);
    String evaluatedPassword = isEmpty(getPassword(password)) ? StringUtils.EMPTY : "--password *******";

    return repoAddCommand.replace(PASSWORD, evaluatedPassword);
  }

  private String getHttpRepoAddCommandWithoutPassword(
      String repoName, String chartRepoUrl, String username, String workingDirectory, HelmVersion helmVersion) {
    String command = HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_ADD_HTTP, helmVersion)
                         .replace(HELM_PATH_PLACEHOLDER, getHelmPath(helmVersion))
                         .replace(REPO_NAME, repoName)
                         .replace(REPO_URL, chartRepoUrl)
                         .replace(USERNAME, getUsername(username));

    return applyHelmHomePath(command, workingDirectory);
  }

  public String applyHelmHomePath(String command, String workingDirectory) {
    if (isBlank(workingDirectory)) {
      return command.replace(HELM_HOME_PATH_FLAG, "");
    } else {
      String helmHomePath = getHelmHomePath(workingDirectory);
      return command.replace(HELM_HOME_PATH_FLAG, "--home " + helmHomePath);
    }
  }

  public String getHelmHomePath(String workingDirectory) {
    return Paths.get(workingDirectory, "helm").normalize().toAbsolutePath().toString();
  }

  public String getHelmPath(HelmVersion helmVersion) {
    return encloseWithQuotesIfNeeded(k8sGlobalConfigService.getHelmPath(helmVersion));
  }

  private String getUsername(String username) {
    return isBlank(username) ? "" : "--username " + username;
  }

  private String getPassword(char[] password) {
    if (password == null) {
      return "";
    }

    String passwordAsString = new String(password);
    return isBlank(passwordAsString) ? "" : "--password " + passwordAsString;
  }

  public ProcessResult executeCommand(String command, String directoryPath, String errorMessage, long timeoutInMillis) {
    errorMessage = isEmpty(errorMessage) ? "" : errorMessage;
    ProcessExecutor processExecutor = createProcessExecutor(command, directoryPath, timeoutInMillis);

    try {
      return processExecutor.execute();
    } catch (IOException e) {
      // Not setting the cause here because it carries forward the commands which can contain passwords
      throw new HelmClientException(format("[IO exception] %s", errorMessage), USER);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HelmClientException(format("[Interrupted] %s", errorMessage), USER);
    } catch (TimeoutException | UncheckedTimeoutException e) {
      throw new HelmClientException(format("[Timed out] %s", errorMessage), USER);
    }
  }

  public ProcessExecutor createProcessExecutor(String command, String directoryPath, long timeoutInMillis) {
    return new ProcessExecutor()
        .directory(isNotBlank(directoryPath) ? new File(directoryPath) : null)
        .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
        .commandSplit(command)
        .readOutput(true);
  }

  public String createNewDirectoryAtPath(String directoryBase) throws IOException {
    String workingDirectory = Paths.get(directoryBase, convertBase64UuidToCanonicalForm(generateUuid()))
                                  .normalize()
                                  .toAbsolutePath()
                                  .toString();

    createDirectoryIfDoesNotExist(workingDirectory);
    waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);

    return workingDirectory;
  }

  public void removeRepo(String repoName, String workingDirectory, HelmVersion helmVersion, long timeoutInMillis) {
    try {
      String repoRemoveCommand = getRepoRemoveCommand(repoName, workingDirectory, helmVersion);

      ProcessResult processResult =
          executeCommand(repoRemoveCommand, null, format("remove helm repo %s", repoName), timeoutInMillis);
      if (processResult.getExitValue() != 0) {
        log.warn("Failed to remove helm repo {}. {}", repoName, processResult.getOutput().getUTF8());
      }
    } catch (Exception ex) {
      log.warn(ExceptionUtils.getMessage(ex));
    }
  }

  private String getRepoRemoveCommand(String repoName, String workingDirectory, HelmVersion helmVersion) {
    String repoRemoveCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_REMOVE, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, getHelmPath(helmVersion))
            .replace(REPO_NAME, repoName);

    return applyHelmHomePath(repoRemoveCommand, workingDirectory);
  }

  public void cleanup(String workingDirectory) {
    try {
      log.info("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      log.warn("Exception in directory cleanup.", ex);
    }
  }

  public String getHelmFetchCommand(String chartName, String chartVersion, String repoName, String workingDirectory,
      HelmVersion helmVersion, HelmCommandFlag helmCommandFlag) {
    HelmCliCommandType commandType = HelmCliCommandType.FETCH;
    String helmFetchCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(commandType, helmVersion)
                                  .replace(HELM_PATH_PLACEHOLDER, getHelmPath(helmVersion))
                                  .replace("${CHART_NAME}", chartName)
                                  .replace("${CHART_VERSION}", getChartVersion(chartVersion));

    if (isNotBlank(repoName)) {
      helmFetchCommand = helmFetchCommand.replace(REPO_NAME, repoName);
    } else {
      helmFetchCommand = helmFetchCommand.replace(REPO_NAME + "/", "");
    }

    Map<HelmSubCommandType, String> commandFlagValueMap =
        helmCommandFlag != null ? helmCommandFlag.getValueMap() : null;
    helmFetchCommand = HelmCommandFlagsUtils.applyHelmCommandFlags(
        helmFetchCommand, commandType.name(), commandFlagValueMap, helmVersion);
    return applyHelmHomePath(helmFetchCommand, workingDirectory);
  }

  public void fetchChartFromRepo(String repoName, String repoDisplayName, String chartName, String chartVersion,
      String chartDirectory, HelmVersion helmVersion, HelmCommandFlag helmCommandFlag, long timeoutInMillis) {
    String helmFetchCommand =
        getHelmFetchCommand(chartName, chartVersion, repoName, chartDirectory, helmVersion, helmCommandFlag);
    executeFetchChartFromRepo(chartName, chartDirectory, repoDisplayName, helmFetchCommand, timeoutInMillis);
  }

  public void executeFetchChartFromRepo(
      String chartName, String chartDirectory, String repoDisplayName, String helmFetchCommand, long timeoutInMillis) {
    log.info(helmFetchCommand);

    ProcessResult processResult =
        executeCommand(helmFetchCommand, chartDirectory, format("fetch chart %s", chartName), timeoutInMillis);
    if (processResult.getExitValue() != 0) {
      StringBuilder builder = new StringBuilder().append("Failed to fetch chart \"").append(chartName).append("\" ");

      if (isNotBlank(repoDisplayName)) {
        builder.append(" from repo \"").append(repoDisplayName).append("\". ");
      }
      builder.append("Please check if the chart is present in the repo.");

      throw new InvalidRequestException(builder.toString(), USER);
    }
  }

  public void downloadChartFilesFromHttpRepo(
      HelmChartManifestDelegateConfig manifest, String destinationDirectory, long timeoutInMillis) {
    if (!(manifest.getStoreDelegateConfig() instanceof HttpHelmStoreDelegateConfig)) {
      throw new InvalidArgumentsException(
          Pair.of("storeDelegateConfig", "Must be instance of HttpHelmStoreDelegateConfig"));
    }

    HttpHelmStoreDelegateConfig storeDelegateConfig = (HttpHelmStoreDelegateConfig) manifest.getStoreDelegateConfig();
    HttpHelmConnectorDTO httpHelmConnector = storeDelegateConfig.getHttpHelmConnector();

    String username = null;
    char[] password = null;
    if (httpHelmConnector.getAuth() != null && USER_PASSWORD == httpHelmConnector.getAuth().getAuthType()) {
      HttpHelmUsernamePasswordDTO usernamePassword =
          (HttpHelmUsernamePasswordDTO) httpHelmConnector.getAuth().getCredentials();
      username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
          usernamePassword.getUsername(), usernamePassword.getUsernameRef());
      password = usernamePassword.getPasswordRef().getDecryptedValue();
    }

    addRepo(storeDelegateConfig.getRepoName(), storeDelegateConfig.getRepoDisplayName(),
        httpHelmConnector.getHelmRepoUrl(), username, password, destinationDirectory, manifest.getHelmVersion(),
        timeoutInMillis);
    fetchChartFromRepo(storeDelegateConfig.getRepoName(), storeDelegateConfig.getRepoDisplayName(),
        manifest.getChartName(), manifest.getChartVersion(), destinationDirectory, manifest.getHelmVersion(),
        manifest.getHelmCommandFlag(), timeoutInMillis);
  }

  public void downloadChartFilesUsingChartMuseum(
      HelmChartManifestDelegateConfig manifest, String destinationDirectory, long timeoutInMillis) throws Exception {
    String resourceDirectory = null;
    ChartMuseumServer chartMuseumServer = null;
    String repoName = null;
    String repoDisplayName = null;
    StoreDelegateConfig storeDelegateConfig = manifest.getStoreDelegateConfig();
    if (S3_HELM == storeDelegateConfig.getType()) {
      S3HelmStoreDelegateConfig s3StoreDelegateConfig = (S3HelmStoreDelegateConfig) storeDelegateConfig;
      repoName = s3StoreDelegateConfig.getRepoName();
      repoDisplayName = s3StoreDelegateConfig.getRepoDisplayName();
    }

    try {
      resourceDirectory = createNewDirectoryAtPath(RESOURCE_DIR_BASE);
      chartMuseumServer =
          ngChartMuseumService.startChartMuseumServer(manifest.getStoreDelegateConfig(), resourceDirectory);

      addChartMuseumRepo(repoName, repoDisplayName, chartMuseumServer.getPort(), destinationDirectory,
          manifest.getHelmVersion(), timeoutInMillis);
      fetchChartFromRepo(repoName, repoDisplayName, manifest.getChartName(), manifest.getChartVersion(),
          destinationDirectory, manifest.getHelmVersion(), manifest.getHelmCommandFlag(), timeoutInMillis);

    } finally {
      if (chartMuseumServer != null) {
        ngChartMuseumService.stopChartMuseumServer(chartMuseumServer);
      }

      if (repoName != null) {
        removeRepo(repoName, destinationDirectory, manifest.getHelmVersion(), timeoutInMillis);
      }

      cleanup(resourceDirectory);
    }
  }

  public void addChartMuseumRepo(String repoName, String repoDisplayName, int port, String chartDirectory,
      HelmVersion helmVersion, long timeoutInMillis) {
    String repoAddCommand = getChartMuseumRepoAddCommand(repoName, port, chartDirectory, helmVersion);
    log.info(repoAddCommand);
    log.info(ADD_COMMAND_FOR_REPOSITORY + repoDisplayName);

    ProcessResult processResult =
        executeCommand(repoAddCommand, chartDirectory, ADD_COMMAND_FOR_REPOSITORY + repoDisplayName, timeoutInMillis);
    if (processResult.getExitValue() != 0) {
      throw new HelmClientException(
          "Failed to add helm repo. Executed command " + repoAddCommand + ". " + processResult.getOutput().getUTF8(),
          USER);
    }
  }

  private String getChartMuseumRepoAddCommand(
      String repoName, int port, String workingDirectory, HelmVersion helmVersion) {
    String repoUrl = CHART_MUSEUM_SERVER_URL.replace("${PORT}", Integer.toString(port));

    String repoAddCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.REPO_ADD_CHART_MEUSEUM, helmVersion)
            .replace(HELM_PATH_PLACEHOLDER, getHelmPath(helmVersion))
            .replace(REPO_NAME, repoName)
            .replace(REPO_URL, repoUrl);

    return applyHelmHomePath(repoAddCommand, workingDirectory);
  }

  public void printHelmChartInfoInExecutionLogs(
      HelmChartManifestDelegateConfig manifestDelegateConfig, LogCallback executionLogCallback) {
    String repoDisplayName = "";
    String basePath = "";
    String chartName = manifestDelegateConfig.getChartName();
    String chartVersion = manifestDelegateConfig.getChartVersion();
    String chartBucket = "";
    String chartRepoUrl = "";

    if (HTTP_HELM == manifestDelegateConfig.getStoreDelegateConfig().getType()) {
      HttpHelmStoreDelegateConfig httpStoreDelegateConfig =
          (HttpHelmStoreDelegateConfig) manifestDelegateConfig.getStoreDelegateConfig();
      repoDisplayName = httpStoreDelegateConfig.getRepoDisplayName();
      chartRepoUrl = httpStoreDelegateConfig.getHttpHelmConnector().getHelmRepoUrl();
    }

    if (isNotBlank(repoDisplayName)) {
      executionLogCallback.saveExecutionLog("Helm repository: " + repoDisplayName);
    }

    if (isNotBlank(basePath)) {
      executionLogCallback.saveExecutionLog("Base Path: " + basePath);
    }

    if (isNotBlank(chartName)) {
      executionLogCallback.saveExecutionLog("Chart name: " + chartName);
    }

    if (isNotBlank(chartVersion)) {
      executionLogCallback.saveExecutionLog("Chart version: " + chartVersion);
    }

    if (manifestDelegateConfig.getHelmVersion() != null) {
      executionLogCallback.saveExecutionLog("Helm version: " + manifestDelegateConfig.getHelmVersion());
    }

    if (isNotBlank(chartBucket)) {
      executionLogCallback.saveExecutionLog("Chart bucket: " + chartBucket);
    }

    if (isNotBlank(chartRepoUrl)) {
      executionLogCallback.saveExecutionLog("Repo url: " + chartRepoUrl);
    }
  }

  private String getChartVersion(String chartVersion) {
    return isBlank(chartVersion) ? StringUtils.EMPTY : "--version " + chartVersion;
  }

  public static String getChartDirectory(String parentDir, String chartName) {
    int lastIndex = chartName.lastIndexOf('/');
    if (lastIndex != -1) {
      return Paths.get(parentDir, chartName.substring(lastIndex + 1)).toString();
    }
    return Paths.get(parentDir, chartName).toString();
  }
}
