/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.helm.HelmConstants.DEFAULT_HELM_COMMAND_TIMEOUT;
import static io.harness.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS;
import static io.harness.helm.HelmConstants.HELM_COMMAND_FLAG_PLACEHOLDER;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;

import static software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper.lockObjects;

import static com.google.common.base.Charsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.helm.HelmCliCommandType;
import io.harness.helm.HelmCommandFlagsUtils;
import io.harness.helm.HelmCommandTemplateFactory;
import io.harness.helm.HelmConstants;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

/**
 * Created by anubhaw on 3/22/18.
 */
@Singleton
@Slf4j
@TargetModule(HarnessModule._960_API_SERVICES)
@OwnedBy(CDP)
public class HelmClientImpl implements HelmClient {
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  private static final String OVERRIDE_FILE_PATH = "./repository/helm/overrides/${CONTENT_HASH}.yaml";

  @Override
  public HelmInstallCommandResponse install(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    String keyValueOverrides = constructValueOverrideFile(commandRequest.getVariableOverridesYamlFiles());
    String chartReference = getChartReference(commandRequest);

    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.INSTALL;
    String installCommand = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                                .replace("${OVERRIDE_VALUES}", keyValueOverrides)
                                .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
                                .replace("${NAMESPACE}", getNamespaceFlag(commandRequest.getNamespace()))
                                .replace("${CHART_REFERENCE}", chartReference);

    installCommand = applyCommandFlags(installCommand, commandRequest, commandType);
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
    HelmCliCommandType commandType = HelmCliCommandType.UPGRADE;
    String upgradeCommand = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                                .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
                                .replace("${CHART_REFERENCE}", chartReference)
                                .replace("${OVERRIDE_VALUES}", keyValueOverrides);

    upgradeCommand = applyCommandFlags(upgradeCommand, commandRequest, commandType);
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
    HelmCliCommandType commandType = HelmCliCommandType.ROLLBACK;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                         .replace("${RELEASE}", commandRequest.getReleaseName())
                         .replace("${REVISION}", commandRequest.getPrevReleaseVersion().toString());

    command = applyCommandFlags(command, commandRequest, commandType);
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

    HelmCliCommandType commandType = HelmCliCommandType.RELEASE_HISTORY;
    String releaseHistory = getHelmCommandTemplateWithHelmPath(commandType, helmCommandRequest.getHelmVersion())
                                .replace("${FLAGS}", "--max 5")
                                .replace("${RELEASE_NAME}", releaseName);

    releaseHistory = applyCommandFlags(releaseHistory, helmCommandRequest, commandType);
    logHelmCommandInExecutionLogs(helmCommandRequest, releaseHistory);
    releaseHistory = applyKubeConfigToCommand(releaseHistory, kubeConfigLocation);

    return executeHelmCLICommand(releaseHistory);
  }

  @Override
  public HelmCliResponse listReleases(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.LIST_RELEASE;
    String listRelease = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                             .replace("${RELEASE_NAME}", commandRequest.getReleaseName());

    listRelease = applyCommandFlags(listRelease, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, listRelease);
    listRelease = applyKubeConfigToCommand(listRelease, kubeConfigLocation);

    try (LogOutputStream errorStream = commandRequest.getExecutionLogCallback() != null
            ? new ErrorActivityOutputStream(commandRequest.getExecutionLogCallback())
            : new LogErrorStream()) {
      return executeHelmCLICommand(listRelease, errorStream);
    }
  }

  @Override
  public HelmCliResponse getClientAndServerVersion(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.VERSION;
    String command = getHelmCommandTemplateWithHelmPath(commandType, null);

    command = applyCommandFlags(command, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command, DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS);
  }

  @Override
  public HelmCliResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.REPO_ADD;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                         .replace("${REPO_URL}", commandRequest.getChartSpecification().getChartUrl())
                         .replace("${REPO_NAME}", commandRequest.getRepoName());

    command = applyCommandFlags(command, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse repoUpdate(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.REPO_UPDATE;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion());

    command = applyCommandFlags(command, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    return executeHelmCLICommand(command);
  }

  @Override
  public HelmCliResponse getHelmRepoList(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    HelmCliCommandType commandType = HelmCliCommandType.REPO_LIST;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion());

    command = applyCommandFlags(command, commandRequest, commandType);
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
    HelmCliCommandType commandType = HelmCliCommandType.DELETE_RELEASE;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                         .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
                         .replace("${FLAGS}", "--purge");

    command = applyCommandFlags(command, commandRequest, commandType);
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

    String templateCommand = HelmConstants.V2Commands.HELM_TEMPLATE_COMMAND_FOR_KUBERNETES_TEMPLATE
                                 .replace("${CHART_LOCATION}", chartLocation)
                                 .replace("${OVERRIDE_VALUES}", keyValueOverrides)
                                 .replace("${RELEASE_NAME}", releaseName)
                                 .replace("${NAMESPACE}", getNamespaceFlag(namespace));

    return executeHelmCLICommand(templateCommand);
  }

  @Override
  public HelmCliResponse renderChart(HelmCommandRequest commandRequest, String chartLocation, String namespace,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException {
    String kubeConfigLocation = Optional.ofNullable(commandRequest.getKubeConfigLocation()).orElse("");
    String keyValueOverrides = constructValueOverrideFile(valuesOverrides);

    HelmCliCommandType commandType = HelmCliCommandType.RENDER_CHART;
    String command = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                         .replace("${CHART_LOCATION}", chartLocation)
                         .replace("${RELEASE_NAME}", commandRequest.getReleaseName())
                         .replace("${NAMESPACE}", namespace)
                         .replace("${OVERRIDE_VALUES}", keyValueOverrides);

    command = applyCommandFlags(command, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, command);
    command = applyKubeConfigToCommand(command, kubeConfigLocation);

    try (LogOutputStream errorStream = commandRequest.getExecutionLogCallback() != null
            ? new ErrorActivityOutputStream(commandRequest.getExecutionLogCallback())
            : new LogErrorStream()) {
      return executeHelmCLICommand(command, errorStream);
    }
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
    HelmCliCommandType commandType = HelmCliCommandType.SEARCH_REPO;
    String searchChartCommand = getHelmCommandTemplateWithHelmPath(commandType, commandRequest.getHelmVersion())
                                    .replace("${CHART_INFO}", chartInfo);

    searchChartCommand = applyCommandFlags(searchChartCommand, commandRequest, commandType);
    logHelmCommandInExecutionLogs(commandRequest, searchChartCommand);
    searchChartCommand = applyKubeConfigToCommand(searchChartCommand, kubeConfigLocation);

    return executeHelmCLICommand(searchChartCommand);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command, OutputStream errorStream)
      throws IOException, InterruptedException, TimeoutException {
    return executeHelmCLICommand(command, DEFAULT_HELM_COMMAND_TIMEOUT, errorStream);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command) throws IOException, InterruptedException, TimeoutException {
    return executeHelmCLICommand(command, DEFAULT_HELM_COMMAND_TIMEOUT, null);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command, long timeoutInMillis)
      throws IOException, InterruptedException, TimeoutException {
    return executeHelmCLICommand(command, timeoutInMillis, null);
  }

  @VisibleForTesting
  HelmCliResponse executeHelmCLICommand(String command, long timeoutInMillis, OutputStream errorStream)
      throws IOException, InterruptedException, TimeoutException {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              log.info(line);
                                            }
                                          });

    if (errorStream != null) {
      processExecutor.redirectError(errorStream);
    }

    ProcessResult processResult = processExecutor.execute();
    CommandExecutionStatus status = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
    return HelmCliResponse.builder().commandExecutionStatus(status).output(processResult.outputUTF8()).build();
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

  private void logHelmCommandInExecutionLogs(HelmCommandRequest helmCommandRequest, String helmCommand) {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    if (executionLogCallback != null) {
      String msg = "Executing command - " + helmCommand + "\n";
      log.info(msg);
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

  private String applyCommandFlags(String command, HelmCommandRequest commandRequest, HelmCliCommandType commandType) {
    String flags = isBlank(commandRequest.getCommandFlags()) ? "" : commandRequest.getCommandFlags();
    if (null != commandRequest.getHelmCommandFlag() && isNotEmpty(commandRequest.getHelmCommandFlag().getValueMap())) {
      return HelmCommandFlagsUtils.applyHelmCommandFlags(command, commandType.name(),
          commandRequest.getHelmCommandFlag().getValueMap(), commandRequest.getHelmVersion());
    }
    return command.replace(HELM_COMMAND_FLAG_PLACEHOLDER, flags);
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
    String helmPath = getHelmPath(helmVersion);
    return HelmCommandTemplateFactory.getHelmCommandTemplate(commandType, helmVersion)
        .replace(HelmConstants.HELM_PATH_PLACEHOLDER, helmPath);
  }

  @Override
  public String getHelmPath(HelmVersion helmVersion) {
    return helmVersion == HelmVersion.V3 ? k8sGlobalConfigService.getHelmPath(HelmVersion.V3) : "helm";
  }

  @RequiredArgsConstructor
  static class ErrorActivityOutputStream extends LogOutputStream {
    @NonNull private final LogCallback logCallback;

    @Override
    protected void processLine(String s) {
      logCallback.saveExecutionLog(s, ERROR);
    }
  }

  static class LogErrorStream extends LogOutputStream {
    @Override
    protected void processLine(String s) {
      log.error(s);
    }
  }
}
