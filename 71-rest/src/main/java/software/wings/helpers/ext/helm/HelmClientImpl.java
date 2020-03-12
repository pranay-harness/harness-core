package software.wings.helpers.ext.helm;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper.lockObjects;
import static software.wings.helpers.ext.helm.HelmCommandTemplateFactory.HelmCliCommandType;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_HELM_COMMAND_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.HelmConstants.V2Commands;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 3/22/18.
 */
@Singleton
@Slf4j
public class HelmClientImpl implements HelmClient {
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  private static final String OVERRIDE_FILE_PATH = "./repository/helm/overrides/${CONTENT_HASH}.yaml";

  @Override
  public HelmInstallCommandResponse install(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    String keyValueOverrides = constructValueOverrideFile(commandRequest.getVariableOverridesYamlFiles());
    String chartReference = getChartReference(commandRequest);

    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String installCommand =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.INSTALL, commandRequest.getHelmVersion())
            .replace("${OVERRIDE_VALUES}", keyValueOverrides)
            .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
            .replace("${NAMESPACE}", getNamespaceFlag(commandRequest.getNamespace()))
            .replace("${CHART_REFERENCE}", chartReference);

    installCommand = applyCommandFlags(installCommand, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, installCommand);
    installCommand = applyKubeConfigToCommand(installCommand, kubeConfigLocation);

    HelmCliResponse cliResponse = executeHelmCLICommand(installCommand);
    return HelmInstallCommandResponse.builder()
        .commandExecutionStatus(cliResponse.getCommandExecutionStatus())
        .output(cliResponse.output)
        .build();
  }

  @Override
  public HelmInstallCommandResponse upgrade(HelmInstallCommandRequest commandRequest)
      throws IOException, ExecutionException, TimeoutException, InterruptedException {
    String keyValueOverrides = constructValueOverrideFile(commandRequest.getVariableOverridesYamlFiles());
    String chartReference = getChartReference(commandRequest);

    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String upgradeCommand =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.UPGRADE, commandRequest.getHelmVersion())
            .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
            .replace("${CHART_REFERENCE}", chartReference)
            .replace("${OVERRIDE_VALUES}", keyValueOverrides);

    upgradeCommand = applyCommandFlags(upgradeCommand, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, upgradeCommand);
    upgradeCommand = applyKubeConfigToCommand(upgradeCommand, kubeConfigLocation);

    HelmCliResponse cliResponse = executeHelmCLICommand(upgradeCommand);
    return HelmInstallCommandResponse.builder()
        .commandExecutionStatus(cliResponse.getCommandExecutionStatus())
        .output(cliResponse.output)
        .build();
  }

  @Override
  public HelmInstallCommandResponse rollback(HelmRollbackCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = getHelmCommandTemplateWithHelmPath(HelmCliCommandType.ROLLBACK, commandRequest.getHelmVersion())
                         .replace("${RELEASE}", commandRequest.getReleaseName())
                         .replace("${REVISION}", commandRequest.getPrevReleaseVersion().toString());

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    HelmCliResponse cliResponse = executeHelmCLICommand(command);
    return HelmInstallCommandResponse.builder()
        .commandExecutionStatus(cliResponse.getCommandExecutionStatus())
        .output(cliResponse.output)
        .build();
  }

  @Override
  public HelmCliResponse releaseHistory(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = helmCommandRequest.getKubeConfigLocation();
    String releaseName = helmCommandRequest.getReleaseName();

    if (kubeConfigLocation == null) {
      kubeConfigLocation = "";
    }

    String releaseHistory =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.RELEASE_HISTORY, helmCommandRequest.getHelmVersion())
            .replace("${FLAGS}", "--max 5")
            .replace("${RELEASE_NAME}", releaseName);

    releaseHistory = applyCommandFlags(releaseHistory, helmCommandRequest);
    logHelmCommandInExecutionLogs(helmCommandRequest, releaseHistory);
    releaseHistory = applyKubeConfigToCommand(releaseHistory, kubeConfigLocation);

    return executeHelmCLICommand(releaseHistory);
  }

  @Override
  public HelmCliResponse listReleases(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String listRelease =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.LIST_RELEASE, commandRequest.getHelmVersion())
            .replace("${RELEASE_NAME}", commandRequest.getReleaseName());

    listRelease = applyCommandFlags(listRelease, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, listRelease);
    listRelease = applyKubeConfigToCommand(listRelease, kubeConfigLocation);

    return executeHelmCLICommand(listRelease);
  }

  @Override
  public HelmCliResponse getClientAndServerVersion(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = getHelmCommandTemplateWithHelmPath(HelmCliCommandType.VERSION, null);

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command, DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS);
  }

  @Override
  public HelmCliResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = getHelmCommandTemplateWithHelmPath(HelmCliCommandType.REPO_ADD, commandRequest.getHelmVersion())
                         .replace("${REPO_URL}", commandRequest.getChartSpecification().getChartUrl())
                         .replace("${REPO_NAME}", commandRequest.getRepoName());

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse repoUpdate(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.REPO_UPDATE, commandRequest.getHelmVersion());

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse getHelmRepoList(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = getHelmCommandTemplateWithHelmPath(HelmCliCommandType.REPO_LIST, commandRequest.getHelmVersion());

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse deleteHelmRelease(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    /*
    In Helm3, release ledger is purged by default unlike helm2
     */
    String command =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.DELETE_RELEASE, commandRequest.getHelmVersion())
            .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
            .replace("${FLAGS}", "--purge");

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  /*
  TODO: YOGESH check why this function is not called and do we need a version here.
   */
  @Override
  public HelmCliResponse templateForK8sV2(String releaseName, String namespace, String chartLocation,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException {
    String keyValueOverrides = constructValueOverrideFile(valuesOverrides);

    String templateCommand =
        V2Commands.HELM_TEMPLATE_COMMAND_FOR_KUBERNETES_TEMPLATE.replace("${CHART_LOCATION}", chartLocation)
            .replace("${OVERRIDE_VALUES}", keyValueOverrides)
            .replace("${RELEASE_NAME}", releaseName)
            .replace("${NAMESPACE}", getNamespaceFlag(namespace));

    return executeHelmCLICommand(templateCommand);
  }

  /**
   * The type Helm cli response.
   */
  @Data
  @Builder
  public static class HelmCliResponse {
    private CommandExecutionStatus commandExecutionStatus;
    private String output;
  }

  @Override
  public HelmCliResponse searchChart(HelmInstallCommandRequest commandRequest, String chartInfo)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String searchChartCommand =
        getHelmCommandTemplateWithHelmPath(HelmCliCommandType.SEARCH_REPO, commandRequest.getHelmVersion())
            .replace("${CHART_INFO}", chartInfo);

    searchChartCommand = applyCommandFlags(searchChartCommand, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, searchChartCommand);
    searchChartCommand = applyKubeConfigToCommand(searchChartCommand, kubeConfigLocation);

    return executeHelmCLICommand(searchChartCommand);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command) throws IOException, InterruptedException, TimeoutException {
    return executeHelmCLICommand(command, DEFAULT_HELM_COMMAND_TIMEOUT);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command, long timeoutInMillis)
      throws IOException, InterruptedException, TimeoutException {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              logger.info(line);
                                            }
                                          });

    ProcessResult processResult = processExecutor.execute();
    CommandExecutionStatus status = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
    return HelmCliResponse.builder().commandExecutionStatus(status).output(processResult.outputString()).build();
  }

  private String getNamespaceFlag(String namespace) {
    return "--namespace " + namespace;
  }

  private String getChartReference(HelmChartSpecification chartSpecification) {
    String chartReference = chartSpecification.getChartName();

    if (isNotEmpty(chartSpecification.getChartUrl())) {
      chartReference = chartReference + " --repo " + chartSpecification.getChartUrl();
    }

    if (isNotEmpty(chartSpecification.getChartVersion())) {
      chartReference = chartReference + " --version " + chartSpecification.getChartVersion();
    }

    return chartReference;
  }

  private String constructValueOverrideFile(List<String> valuesYamlFiles) throws IOException, ExecutionException {
    StringBuilder fileOverrides = new StringBuilder();
    if (isNotEmpty(valuesYamlFiles)) {
      for (String yamlFileContent : valuesYamlFiles) {
        String md5Hash = DigestUtils.md5Hex(yamlFileContent);
        String overrideFilePath = OVERRIDE_FILE_PATH.replace("${CONTENT_HASH}", md5Hash);

        synchronized (lockObjects.get(md5Hash)) {
          File overrideFile = new File(overrideFilePath);
          if (!overrideFile.exists()) {
            FileUtils.forceMkdir(overrideFile.getParentFile());
            FileUtils.writeStringToFile(overrideFile, yamlFileContent, UTF_8);
          }
          fileOverrides.append(" -f ").append(overrideFilePath);
        }
      }
    }
    return fileOverrides.toString();
  }

  private String dryRunCommand(String command) {
    return command + "--dry-run";
  }

  private void logHelmCommandInExecutionLogs(HelmCommandRequest helmCommandRequest, String helmCommand) {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    if (executionLogCallback != null) {
      String msg = "Executing command - " + helmCommand + "\n";
      logger.info(msg);
      executionLogCallback.saveExecutionLog(msg, LogLevel.INFO);
    }
  }

  private String applyKubeConfigToCommand(String command, String kubeConfigLocation) {
    if (isNotEmpty(kubeConfigLocation)) {
      return command.replace("${KUBECONFIG_PATH}", kubeConfigLocation);
    } else {
      return command.replace("KUBECONFIG=${KUBECONFIG_PATH}", EMPTY).trim();
    }
  }

  private String applyCommandFlags(String command, HelmCommandRequest commandRequest) {
    String flags = isBlank(commandRequest.getCommandFlags()) ? "" : commandRequest.getCommandFlags();

    return command.replace("${COMMAND_FLAGS}", flags);
  }

  private String getChartReference(HelmInstallCommandRequest commandRequest) {
    String chartReference;
    if (commandRequest.getRepoConfig() == null) {
      chartReference = getChartReference(commandRequest.getChartSpecification());
    } else {
      chartReference = Paths.get(commandRequest.getWorkingDir()).toString();
    }
    return chartReference;
  }

  private String getHelmCommandTemplateWithHelmPath(HelmCliCommandType commandType, HelmVersion helmVersion) {
    String helmPath = helmVersion == HelmVersion.V3 ? k8sGlobalConfigService.getHelmPath(HelmVersion.V3) : "helm";
    return HelmCommandTemplateFactory.getHelmCommandTemplate(commandType, helmVersion)
        .replace(HelmConstants.HELM_PATH_PLACEHOLDER, helmPath);
  }
}
