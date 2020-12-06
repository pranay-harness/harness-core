package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.govern.Switch.unhandled;
import static io.harness.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.beans.LogWeight.Normal;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileData;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitFile;
import io.harness.helm.HelmCommandTemplateFactory;
import io.harness.helm.HelmCommandTemplateFactory.HelmCliCommandType;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmCommandFlag;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;
import software.wings.helpers.ext.kustomize.KustomizeTaskHelper;
import software.wings.helpers.ext.openshift.OpenShiftDelegateService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.CommandFlagUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Singleton
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class K8sTaskHelper {
  @Inject protected DelegateLogService delegateLogService;
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmTaskHelper helmTaskHelper;
  @Inject private KustomizeTaskHelper kustomizeTaskHelper;
  @Inject private OpenShiftDelegateService openShiftDelegateService;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private HelmHelper helmHelper;

  public boolean doStatusCheckAllResourcesForHelm(Kubectl client, List<KubernetesResourceId> resourceIds, String ocPath,
      String workingDir, String namespace, String kubeconfigPath, ExecutionLogCallback executionLogCallback)
      throws Exception {
    return k8sTaskHelperBase.doStatusCheckForAllResources(client, resourceIds,
        K8sDelegateTaskParams.builder()
            .ocPath(ocPath)
            .workingDirectory(workingDir)
            .kubeconfigPath(kubeconfigPath)
            .build(),
        namespace, executionLogCallback, false);
  }

  private boolean writeManifestFilesToDirectory(
      List<ManifestFile> manifestFiles, String manifestFilesDirectory, ExecutionLogCallback executionLogCallback) {
    String directoryPath = Paths.get(manifestFilesDirectory).toString();

    try {
      for (int i = 0; i < manifestFiles.size(); i++) {
        ManifestFile manifestFile = manifestFiles.get(i);
        if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
          continue;
        }

        Path filePath = Paths.get(directoryPath, manifestFile.getFileName());
        Path parent = filePath.getParent();
        if (parent == null) {
          throw new WingsException("Failed to create file at path " + filePath.toString());
        }

        createDirectoryIfDoesNotExist(parent.toString());
        FileIo.writeUtf8StringToFile(filePath.toString(), manifestFile.getFileContent());
      }

      return true;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(ex), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public List<FileData> renderTemplateForHelm(String helmPath, String manifestFilesDirectory, List<String> valuesFiles,
      String releaseName, String namespace, LogCallback executionLogCallback, HelmVersion helmVersion,
      long timeoutInMillis, HelmCommandFlag helmCommandFlag) throws Exception {
    String valuesFileOptions = k8sTaskHelperBase.writeValuesToFile(manifestFilesDirectory, valuesFiles);
    log.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<FileData> result = new ArrayList<>();
    try (LogOutputStream logErrorStream = K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, ERROR)) {
      String helmTemplateCommand = getHelmCommandForRender(
          helmPath, manifestFilesDirectory, releaseName, namespace, valuesFileOptions, helmVersion, helmCommandFlag);
      printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

      ProcessResult processResult = k8sTaskHelperBase.executeShellCommand(
          manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
      if (processResult.getExitValue() != 0) {
        throw new WingsException(format("Failed to render helm chart. Error %s", processResult.getOutput().getUTF8()));
      }

      result.add(FileData.builder().fileName("manifest.yaml").fileContent(processResult.outputUTF8()).build());
    }

    return result;
  }

  public List<FileData> renderTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory, List<String> valuesFiles,
      String releaseName, String namespace, ExecutionLogCallback executionLogCallback,
      K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());
    HelmCommandFlag helmCommandFlag = k8sDelegateManifestConfig.getHelmCommandFlag();

    switch (storeType) {
      case Local:
      case Remote:
        List<FileData> manifestFiles = k8sTaskHelperBase.readManifestFilesFromDirectory(manifestFilesDirectory);
        return k8sTaskHelperBase.renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis,
            helmCommandFlag);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis,
            helmCommandFlag);

      case KustomizeSourceRepo:
        return kustomizeTaskHelper.build(manifestFilesDirectory, k8sDelegateTaskParams.getKustomizeBinaryPath(),
            k8sDelegateManifestConfig.getKustomizeConfig(), executionLogCallback);
      case OC_TEMPLATES:
        return openShiftDelegateService.processTemplatization(manifestFilesDirectory, k8sDelegateTaskParams.getOcPath(),
            k8sDelegateManifestConfig.getGitFileConfig().getFilePath(), executionLogCallback, valuesFiles);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  public List<FileData> renderTemplateForGivenFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());
    HelmCommandFlag helmCommandFlag = k8sDelegateManifestConfig.getHelmCommandFlag();

    switch (storeType) {
      case Local:
      case Remote:
        List<FileData> manifestFiles =
            k8sTaskHelperBase.readFilesFromDirectory(manifestFilesDirectory, filesList, executionLogCallback);
        return k8sTaskHelperBase.renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis,
            helmCommandFlag);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis,
            helmCommandFlag);
      case KustomizeSourceRepo:
        return kustomizeTaskHelper.buildForApply(k8sDelegateTaskParams.getKustomizeBinaryPath(),
            k8sDelegateManifestConfig.getKustomizeConfig(), manifestFilesDirectory, filesList, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  public List<KubernetesResource> getResourcesFromManifests(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    List<FileData> manifestFiles =
        renderTemplateForGivenFiles(k8sDelegateTaskParams, k8sDelegateManifestConfig, manifestFilesDirectory, filesList,
            valuesFiles, releaseName, namespace, executionLogCallback, k8sTaskParameters);
    if (isEmpty(manifestFiles)) {
      return new ArrayList<>();
    }

    List<KubernetesResource> resources = k8sTaskHelperBase.readManifests(manifestFiles, executionLogCallback);
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, namespace);

    return resources;
  }

  private void printGitConfigInExecutionLogs(
      GitConfig gitConfig, GitFileConfig gitFileConfig, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("\n" + color("Fetching manifest files", White, Bold));
    executionLogCallback.saveExecutionLog("Git connector Url: " + gitConfig.getRepoUrl());
    if (gitFileConfig.isUseBranch()) {
      executionLogCallback.saveExecutionLog("Branch: " + gitFileConfig.getBranch());
    } else {
      executionLogCallback.saveExecutionLog("CommitId: " + gitFileConfig.getCommitId());
    }
    executionLogCallback.saveExecutionLog("\nFetching manifest files at path: "
        + (isBlank(gitFileConfig.getFilePath()) ? "." : gitFileConfig.getFilePath()));
  }

  private boolean downloadManifestFilesFromGit(K8sDelegateManifestConfig delegateManifestConfig,
      String manifestFilesDirectory, ExecutionLogCallback executionLogCallback) {
    if (isBlank(delegateManifestConfig.getGitFileConfig().getFilePath())) {
      delegateManifestConfig.getGitFileConfig().setFilePath(StringUtils.EMPTY);
    }

    try {
      GitFileConfig gitFileConfig = delegateManifestConfig.getGitFileConfig();
      GitConfig gitConfig = delegateManifestConfig.getGitConfig();
      printGitConfigInExecutionLogs(gitConfig, gitFileConfig, executionLogCallback);

      encryptionService.decrypt(gitConfig, delegateManifestConfig.getEncryptedDataDetails(), false);

      gitService.downloadFiles(gitConfig, gitFileConfig, manifestFilesDirectory);

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(k8sTaskHelperBase.getManifestFileNamesInLogFormat(manifestFilesDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      log.error("Failure in fetching files from git", e);
      executionLogCallback.saveExecutionLog(
          "Failed to download manifest files from git. " + ExceptionUtils.getMessage(e), ERROR,
          CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public boolean fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig delegateManifestConfig,
      String manifestFilesDirectory, ExecutionLogCallback executionLogCallback, long timeoutInMillis) {
    StoreType storeType = delegateManifestConfig.getManifestStoreTypes();
    switch (storeType) {
      case Local:
        return writeManifestFilesToDirectory(
            delegateManifestConfig.getManifestFiles(), manifestFilesDirectory, executionLogCallback);

      case OC_TEMPLATES:
      case Remote:
      case HelmSourceRepo:
      case KustomizeSourceRepo:
        return downloadManifestFilesFromGit(delegateManifestConfig, manifestFilesDirectory, executionLogCallback);

      case HelmChartRepo:
        return downloadFilesFromChartRepo(
            delegateManifestConfig, manifestFilesDirectory, executionLogCallback, timeoutInMillis);

      default:
        unhandled(storeType);
    }

    return false;
  }

  public HelmChartInfo getHelmChartDetails(K8sDelegateManifestConfig delegateManifestConfig, String workingDirectory) {
    HelmChartInfo helmChartInfo = null;
    try {
      if (delegateManifestConfig != null) {
        StoreType manifestStoreType = delegateManifestConfig.getManifestStoreTypes();
        if (StoreType.HelmSourceRepo == manifestStoreType || StoreType.HelmChartRepo == manifestStoreType) {
          String chartName = Optional.ofNullable(delegateManifestConfig.getHelmChartConfigParams())
                                 .map(HelmChartConfigParams::getChartName)
                                 .orElse("");
          helmChartInfo =
              helmTaskHelper.getHelmChartInfoFromChartDirectory(Paths.get(workingDirectory, chartName).toString());
          if (delegateManifestConfig.getHelmChartConfigParams() != null) {
            helmChartInfo.setRepoUrl(
                helmHelper.getRepoUrlForHelmRepoConfig(delegateManifestConfig.getHelmChartConfigParams()));
          }
        }
      }
    } catch (IOException ex) {
      log.error("Error while fetching helm chart info", ex);
    }

    return helmChartInfo;
  }

  public static List<ManifestFile> manifestFilesFromGitFetchFilesResult(
      GitFetchFilesResult gitFetchFilesResult, String prefixPath) {
    List<ManifestFile> manifestFiles = new ArrayList<>();

    if (isNotEmpty(gitFetchFilesResult.getFiles())) {
      List<GitFile> files = gitFetchFilesResult.getFiles();

      for (GitFile gitFile : files) {
        String filePath = K8sTaskHelperBase.getRelativePath(gitFile.getFilePath(), prefixPath);
        manifestFiles.add(ManifestFile.builder().fileName(filePath).fileContent(gitFile.getFileContent()).build());
      }
    }

    return manifestFiles;
  }

  public K8sTaskExecutionResponse getK8sTaskExecutionResponse(
      K8sTaskResponse k8sTaskResponse, CommandExecutionStatus commandExecutionStatus) {
    return K8sTaskExecutionResponse.builder()
        .k8sTaskResponse(k8sTaskResponse)
        .commandExecutionStatus(commandExecutionStatus)
        .build();
  }

  public ExecutionLogCallback getExecutionLogCallback(K8sTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  private boolean downloadFilesFromChartRepo(K8sDelegateManifestConfig delegateManifestConfig,
      String destinationDirectory, ExecutionLogCallback executionLogCallback, long timeoutInMillis) {
    HelmChartConfigParams helmChartConfigParams = delegateManifestConfig.getHelmChartConfigParams();

    try {
      executionLogCallback.saveExecutionLog(color(format("%nFetching files from helm chart repo"), White, Bold));
      helmTaskHelper.printHelmChartInfoInExecutionLogs(helmChartConfigParams, executionLogCallback);

      helmTaskHelper.downloadChartFiles(
          helmChartConfigParams, destinationDirectory, timeoutInMillis, delegateManifestConfig.getHelmCommandFlag());

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(k8sTaskHelperBase.getManifestFileNamesInLogFormat(destinationDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  @VisibleForTesting
  List<FileData> renderTemplateForHelmChartFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      String manifestFilesDirectory, List<String> chartFiles, List<String> valuesFiles, String releaseName,
      String namespace, LogCallback executionLogCallback, HelmVersion helmVersion, long timeoutInMillis,
      HelmCommandFlag helmCommandFlag) throws Exception {
    String valuesFileOptions = k8sTaskHelperBase.writeValuesToFile(manifestFilesDirectory, valuesFiles);
    String helmPath = k8sDelegateTaskParams.getHelmPath();
    log.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<FileData> result = new ArrayList<>();

    for (String chartFile : chartFiles) {
      if (K8sTaskHelperBase.isValidManifestFile(chartFile)) {
        try (LogOutputStream logErrorStream =
                 K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, ERROR)) {
          String helmTemplateCommand = getHelmCommandForRender(helmPath, manifestFilesDirectory, releaseName, namespace,
              valuesFileOptions, chartFile, helmVersion, helmCommandFlag);
          printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

          ProcessResult processResult = k8sTaskHelperBase.executeShellCommand(
              manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
          if (processResult.getExitValue() != 0) {
            throw new WingsException(format("Failed to render chart file [%s]", chartFile));
          }

          result.add(FileData.builder().fileName(chartFile).fileContent(processResult.outputUTF8()).build());
        }
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", chartFile), Yellow, Bold));
      }
    }

    return result;
  }

  private void printHelmPath(LogCallback executionLogCallback, final String helmPath) {
    executionLogCallback.saveExecutionLog(color("Rendering chart files using Helm", White, Bold));
    executionLogCallback.saveExecutionLog(color(format("Using helm binary %s", helmPath), White, Normal));
  }

  private void printHelmTemplateCommand(LogCallback executionLogCallback, final String helmTemplateCommand) {
    executionLogCallback.saveExecutionLog(color("Running Helm command", White, Bold));
    executionLogCallback.saveExecutionLog(color(helmTemplateCommand, White, Normal));
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, String chartFile, HelmVersion helmVersion, HelmCommandFlag helmCommandFlag) {
    HelmCliCommandType commandType = HelmCliCommandType.RENDER_SPECIFIC_CHART_FILE;
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(commandType, helmVersion);
    String command = replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, chartFile, valuesFileOptions);
    command = CommandFlagUtils.applyHelmCommandFlags(command, helmCommandFlag, commandType.name(), helmVersion);
    return command;
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, HelmVersion helmVersion, HelmCommandFlag commandFlag) {
    HelmCliCommandType commandType = HelmCliCommandType.RENDER_CHART;
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(commandType, helmVersion);
    String command = replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, EMPTY, valuesFileOptions);
    command = CommandFlagUtils.applyHelmCommandFlags(command, commandFlag, commandType.name(), helmVersion);
    return command;
  }

  private String replacePlaceHoldersInHelmTemplateCommand(String unrenderedCommand, String helmPath,
      String chartLocation, String releaseName, String namespace, String chartFile, String valueOverrides) {
    return unrenderedCommand.replace(HELM_PATH_PLACEHOLDER, helmPath)
        .replace("${CHART_LOCATION}", chartLocation)
        .replace("${CHART_FILE}", chartFile)
        .replace("${RELEASE_NAME}", releaseName)
        .replace("${NAMESPACE}", namespace)
        .replace("${OVERRIDE_VALUES}", valueOverrides);
  }

  public List<KubernetesResourceId> getResourceIdsForDeletion(K8sDeleteTaskParameters k8sDeleteTaskParameters,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    List<KubernetesResourceId> kubernetesResourceIds = k8sTaskHelperBase.fetchAllResourcesForRelease(
        k8sDeleteTaskParameters.getReleaseName(), kubernetesConfig, executionLogCallback);

    // If namespace deletion is NOT selected,remove all Namespace resources from deletion list
    if (!k8sDeleteTaskParameters.isDeleteNamespacesForRelease()) {
      kubernetesResourceIds =
          kubernetesResourceIds.stream()
              .filter(kubernetesResourceId -> !Namespace.name().equals(kubernetesResourceId.getKind()))
              .collect(toList());
    }

    return k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(kubernetesResourceIds);
  }
}
