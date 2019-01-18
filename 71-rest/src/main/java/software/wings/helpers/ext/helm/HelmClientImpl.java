package software.wings.helpers.ext.helm;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper.lockObjects;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_HELM_COMMAND_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_ADD_REPO_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DELETE_RELEASE_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_INSTALL_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_LIST_RELEASE_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_RELEASE_HIST_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_REPO_LIST_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_ROLLBACK_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_UPGRADE_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_VERSION_COMMAND_TEMPLATE;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 3/22/18.
 */
@Singleton
public class HelmClientImpl implements HelmClient {
  private static final Logger logger = LoggerFactory.getLogger(HelmClientImpl.class);
  private static final String OVERRIDE_FILE_PATH = "./repository/helm/overrides/${CONTENT_HASH}.yaml";

  @Override
  public HelmInstallCommandResponse install(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    String keyValueOverrides = constructValueOverrideFile(commandRequest);
    String chartReference = getChartReference(commandRequest.getChartSpecification());

    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String installCommand = HELM_INSTALL_COMMAND_TEMPLATE.replace("${OVERRIDE_VALUES}", keyValueOverrides)
                                .replace("${RELEASE_NAME}", getReleaseFlag(commandRequest))
                                .replace("${NAMESPACE}", getNamespaceFlag(commandRequest))
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
    String keyValueOverrides = constructValueOverrideFile(commandRequest);
    String chartReference = getChartReference(commandRequest.getChartSpecification());

    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String upgradeCommand = HELM_UPGRADE_COMMAND_TEMPLATE.replace("${RELEASE_NAME}", commandRequest.getReleaseName())
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
    String command = HELM_ROLLBACK_COMMAND_TEMPLATE.replace("${RELEASE}", commandRequest.getReleaseName())
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
        HELM_RELEASE_HIST_COMMAND_TEMPLATE.replace("${FLAGS}", "--max 5").replace("${RELEASE_NAME}", releaseName);

    releaseHistory = applyCommandFlags(releaseHistory, helmCommandRequest);
    logHelmCommandInExecutionLogs(helmCommandRequest, releaseHistory);
    releaseHistory = applyKubeConfigToCommand(releaseHistory, kubeConfigLocation);

    return executeHelmCLICommand(releaseHistory);
  }

  @Override
  public HelmCliResponse listReleases(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String listRelease = HELM_LIST_RELEASE_COMMAND_TEMPLATE.replace("${RELEASE_NAME}", commandRequest.getReleaseName());

    listRelease = applyCommandFlags(listRelease, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, listRelease);
    listRelease = applyKubeConfigToCommand(listRelease, kubeConfigLocation);

    return executeHelmCLICommand(listRelease);
  }

  @Override
  public HelmCliResponse getClientAndServerVersion(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = HELM_VERSION_COMMAND_TEMPLATE;

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command =
        HELM_ADD_REPO_COMMAND_TEMPLATE.replace("${REPO_URL}", commandRequest.getChartSpecification().getChartUrl())
            .replace("${REPO_NAME}", commandRequest.getRepoName());

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse getHelmRepoList(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = HELM_REPO_LIST_COMMAND_TEMPLATE;

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse deleteHelmRelease(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String command = HELM_DELETE_RELEASE_TEMPLATE.replace("${RELEASE_NAME}", commandRequest.getReleaseName())
                         .replace("${FLAGS}", "--purge");

    command = applyCommandFlags(command, commandRequest);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  private HelmCliResponse executeHelmCLICommand(String command)
      throws IOException, InterruptedException, TimeoutException {
    logger.info("Execution Helm command [{}]", command); // TODO:: remove it later
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(DEFAULT_HELM_COMMAND_TIMEOUT, TimeUnit.MILLISECONDS)
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

  private String getReleaseFlag(HelmInstallCommandRequest requestParameters) {
    return "--name " + requestParameters.getReleaseName();
  }

  private String getNamespaceFlag(HelmInstallCommandRequest requestParameters) {
    return "--namespace " + requestParameters.getNamespace();
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

  private String constructValueOverrideFile(HelmInstallCommandRequest requestParameters)
      throws IOException, ExecutionException {
    StringBuilder fileOverrides = new StringBuilder();
    if (isNotEmpty(requestParameters.getVariableOverridesYamlFiles())) {
      for (String yamlFileContent : requestParameters.getVariableOverridesYamlFiles()) {
        String md5Hash = DigestUtils.md5Hex(yamlFileContent);
        String overrideFilePath = OVERRIDE_FILE_PATH.replace("${CONTENT_HASH}", md5Hash);

        synchronized (lockObjects.get(md5Hash)) {
          File overrideFile = new File(overrideFilePath);
          if (!overrideFile.exists()) {
            FileUtils.forceMkdir(overrideFile.getParentFile());
            FileUtils.writeStringToFile(overrideFile, yamlFileContent, UTF_8);
          }
          fileOverrides.append(" -f").append(overrideFilePath);
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
    return command.replace("${KUBECONFIG_PATH}", kubeConfigLocation);
  }

  private String applyCommandFlags(String command, HelmCommandRequest commandRequest) {
    String flags = isBlank(commandRequest.getCommandFlags()) ? "" : commandRequest.getCommandFlags();

    return command.replace("${COMMAND_FLAGS}", flags);
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
}
