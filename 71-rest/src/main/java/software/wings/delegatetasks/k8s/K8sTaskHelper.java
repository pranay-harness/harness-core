package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.govern.Switch.unhandled;
import static io.harness.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static io.harness.k8s.KubernetesConvention.ReleaseHistoryKeyName;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.manifest.ManifestHelper.validateValuesFileContents;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.k8s.manifest.ManifestHelper.yaml_file_extension;
import static io.harness.k8s.manifest.ManifestHelper.yml_file_extension;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.beans.LogWeight.Normal;
import static software.wings.delegatetasks.k8s.K8sTask.KUBECONFIG_FILENAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.harness.beans.FileData;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.KubernetesValuesException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmCommandTemplateFactory;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Slf4j
public class K8sTaskHelper {
  @Inject protected DelegateLogService delegateLogService;
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmTaskHelper helmTaskHelper;
  @Inject private KustomizeTaskHelper kustomizeTaskHelper;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;
  @Inject private OpenShiftDelegateService openShiftDelegateService;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  public boolean doHelmStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      HelmCommandRequest helmInstallCommandRequest, ExecutionLogCallback executionLogCallback) throws Exception {
    return k8sTaskHelperBase.doStatusCheck(client, resourceId, helmInstallCommandRequest.getWorkingDir(),
        helmInstallCommandRequest.getOcPath(), KUBECONFIG_FILENAME, executionLogCallback);
  }

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

  private String writeValuesToFile(String directoryPath, List<String> valuesFiles) throws Exception {
    StringBuilder valuesFilesOptionsBuilder = new StringBuilder(128);

    for (int i = 0; i < valuesFiles.size(); i++) {
      validateValuesFileContents(valuesFiles.get(i));
      String valuesFileName = format("values-%d.yaml", i);
      FileIo.writeUtf8StringToFile(directoryPath + '/' + valuesFileName, valuesFiles.get(i));
      valuesFilesOptionsBuilder.append(" -f ").append(valuesFileName);
    }

    return valuesFilesOptionsBuilder.toString();
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

  public List<ManifestFile> renderTemplateForHelm(String helmPath, String manifestFilesDirectory,
      List<String> valuesFiles, String releaseName, String namespace, ExecutionLogCallback executionLogCallback,
      HelmVersion helmVersion, long timeoutInMillis) throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    logger.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<ManifestFile> result = new ArrayList<>();
    try (LogOutputStream logErrorStream = K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, ERROR)) {
      String helmTemplateCommand = getHelmCommandForRender(
          helmPath, manifestFilesDirectory, releaseName, namespace, valuesFileOptions, helmVersion);
      printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

      ProcessResult processResult = k8sTaskHelperBase.executeShellCommand(
          manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
      if (processResult.getExitValue() != 0) {
        throw new WingsException(format("Failed to render helm chart. Error %s", processResult.getOutput().getUTF8()));
      }

      result.add(ManifestFile.builder().fileName("manifest.yaml").fileContent(processResult.outputUTF8()).build());
    }

    return result;
  }

  private List<ManifestFile> readManifestFilesFromDirectory(String manifestFilesDirectory) {
    List<FileData> fileDataList;
    Path directory = Paths.get(manifestFilesDirectory);

    try {
      fileDataList = getFilesUnderPath(directory.toString());
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      throw new WingsException("Failed to get files. Error: " + ExceptionUtils.getMessage(ex));
    }

    List<ManifestFile> manifestFiles = new ArrayList<>();
    for (FileData fileData : fileDataList) {
      if (isValidManifestFile(fileData.getFilePath())) {
        manifestFiles.add(ManifestFile.builder()
                              .fileName(fileData.getFilePath())
                              .fileContent(new String(fileData.getFileBytes(), StandardCharsets.UTF_8))
                              .build());
      } else {
        logger.info("Found file [{}] with unsupported extension", fileData.getFilePath());
      }
    }

    return manifestFiles;
  }

  private List<ManifestFile> renderManifestFilesForGoTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      List<ManifestFile> manifestFiles, List<String> valuesFiles, ExecutionLogCallback executionLogCallback,
      long timeoutInMillis) throws Exception {
    if (isEmpty(valuesFiles)) {
      executionLogCallback.saveExecutionLog("No values.yaml file found. Skipping template rendering.");
      return manifestFiles;
    }

    String valuesFileOptions = null;
    try {
      valuesFileOptions = writeValuesToFile(k8sDelegateTaskParams.getWorkingDirectory(), valuesFiles);
    } catch (KubernetesValuesException kvexception) {
      String message = kvexception.getParams().get("reason").toString();
      executionLogCallback.saveExecutionLog(message, ERROR);
      throw new KubernetesValuesException(message, kvexception.getCause());
    }

    logger.info("Values file options: " + valuesFileOptions);

    List<ManifestFile> result = new ArrayList<>();

    executionLogCallback.saveExecutionLog(color("\nRendering manifest files using go template", White, Bold));
    executionLogCallback.saveExecutionLog(
        color("Only manifest files with [.yaml] or [.yml] extension will be processed", White, Bold));

    for (ManifestFile manifestFile : manifestFiles) {
      if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
        continue;
      }

      FileIo.writeUtf8StringToFile(
          k8sDelegateTaskParams.getWorkingDirectory() + "/template.yaml", manifestFile.getFileContent());

      try (
          LogOutputStream logErrorStream = K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, ERROR)) {
        String goTemplateCommand = encloseWithQuotesIfNeeded(k8sDelegateTaskParams.getGoTemplateClientPath())
            + " -t template.yaml " + valuesFileOptions;
        ProcessResult processResult = k8sTaskHelperBase.executeShellCommand(
            k8sDelegateTaskParams.getWorkingDirectory(), goTemplateCommand, logErrorStream, timeoutInMillis);

        if (processResult.getExitValue() != 0) {
          throw new InvalidRequestException(format("Failed to render template for %s. Error %s",
                                                manifestFile.getFileName(), processResult.getOutput().getUTF8()),
              USER);
        }

        result.add(ManifestFile.builder()
                       .fileName(manifestFile.getFileName())
                       .fileContent(processResult.outputUTF8())
                       .build());
      }
    }

    return result;
  }

  public List<ManifestFile> renderTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory, List<String> valuesFiles,
      String releaseName, String namespace, ExecutionLogCallback executionLogCallback,
      K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles = readManifestFilesFromDirectory(manifestFilesDirectory);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

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

  public List<ManifestFile> renderTemplateForGivenFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles =
            readFilesFromDirectory(manifestFilesDirectory, filesList, executionLogCallback);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);
      case KustomizeSourceRepo:
        return kustomizeTaskHelper.buildForApply(k8sDelegateTaskParams.getKustomizeBinaryPath(),
            k8sDelegateManifestConfig.getKustomizeConfig(), manifestFilesDirectory, filesList, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  private static boolean isValidManifestFile(String filename) {
    return (StringUtils.endsWith(filename, yaml_file_extension) || StringUtils.endsWith(filename, yml_file_extension))
        && !StringUtils.equals(filename, values_filename);
  }

  public List<KubernetesResource> readManifestAndOverrideLocalSecrets(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback, boolean overrideLocalSecrets) {
    if (overrideLocalSecrets) {
      replaceManifestPlaceholdersWithLocalDelegateSecrets(manifestFiles);
    }
    return readManifests(manifestFiles, executionLogCallback);
  }

  public List<KubernetesResource> getResourcesFromManifests(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    List<ManifestFile> manifestFiles =
        renderTemplateForGivenFiles(k8sDelegateTaskParams, k8sDelegateManifestConfig, manifestFilesDirectory, filesList,
            valuesFiles, releaseName, namespace, executionLogCallback, k8sTaskParameters);
    if (isEmpty(manifestFiles)) {
      return new ArrayList<>();
    }

    List<KubernetesResource> resources = readManifests(manifestFiles, executionLogCallback);
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, namespace);

    return resources;
  }
  public List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (isValidManifestFile(manifestFile.getFileName())) {
        try {
          result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          throw e;
        }
      }
    }

    return result.stream().sorted(new KubernetesResourceComparer()).collect(toList());
  }

  private void replaceManifestPlaceholdersWithLocalDelegateSecrets(List<ManifestFile> manifestFiles) {
    for (ManifestFile manifestFile : manifestFiles) {
      manifestFile.setFileContent(
          delegateLocalConfigService.replacePlaceholdersWithLocalConfig(manifestFile.getFileContent()));
    }
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

      encryptionService.decrypt(gitConfig, delegateManifestConfig.getEncryptedDataDetails());

      gitService.downloadFiles(gitConfig, gitFileConfig, manifestFilesDirectory);

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(k8sTaskHelperBase.getManifestFileNamesInLogFormat(manifestFilesDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      logger.error("Failure in fetching files from git", e);
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
        }
      }
    } catch (IOException ex) {
      logger.error("Error while fetching helm chart info", ex);
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

      helmTaskHelper.downloadChartFiles(helmChartConfigParams, destinationDirectory, timeoutInMillis);

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(k8sTaskHelperBase.getManifestFileNamesInLogFormat(destinationDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  private List<ManifestFile> readFilesFromDirectory(
      String directory, List<String> filePaths, ExecutionLogCallback executionLogCallback) {
    List<ManifestFile> manifestFiles = new ArrayList<>();

    for (String filepath : filePaths) {
      if (isValidManifestFile(filepath)) {
        Path path = Paths.get(directory, filepath);
        byte[] fileBytes;

        try {
          fileBytes = Files.readAllBytes(path);
        } catch (Exception ex) {
          logger.info(ExceptionUtils.getMessage(ex));
          throw new InvalidRequestException(
              format("Failed to read file at path [%s].%nError: %s", filepath, ExceptionUtils.getMessage(ex)));
        }

        manifestFiles.add(ManifestFile.builder()
                              .fileName(filepath)
                              .fileContent(new String(fileBytes, StandardCharsets.UTF_8))
                              .build());
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", filepath), Yellow, Bold));
      }
    }

    return manifestFiles;
  }

  private List<ManifestFile> renderTemplateForHelmChartFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      String manifestFilesDirectory, List<String> chartFiles, List<String> valuesFiles, String releaseName,
      String namespace, ExecutionLogCallback executionLogCallback, HelmVersion helmVersion, long timeoutInMillis)
      throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    String helmPath = k8sDelegateTaskParams.getHelmPath();
    logger.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<ManifestFile> result = new ArrayList<>();

    for (String chartFile : chartFiles) {
      if (isValidManifestFile(chartFile)) {
        try (LogOutputStream logErrorStream =
                 K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, ERROR)) {
          String helmTemplateCommand = getHelmCommandForRender(
              helmPath, manifestFilesDirectory, releaseName, namespace, valuesFileOptions, chartFile, helmVersion);
          printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

          ProcessResult processResult = k8sTaskHelperBase.executeShellCommand(
              manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
          if (processResult.getExitValue() != 0) {
            throw new WingsException(format("Failed to render chart file [%s]", chartFile));
          }

          result.add(ManifestFile.builder().fileName(chartFile).fileContent(processResult.outputUTF8()).build());
        }
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", chartFile), Yellow, Bold));
      }
    }

    return result;
  }

  private void printHelmPath(ExecutionLogCallback executionLogCallback, final String helmPath) {
    executionLogCallback.saveExecutionLog(color("Rendering chart files using Helm", White, Bold));
    executionLogCallback.saveExecutionLog(color(format("Using helm binary %s", helmPath), White, Normal));
  }

  private void printHelmTemplateCommand(ExecutionLogCallback executionLogCallback, final String helmTemplateCommand) {
    executionLogCallback.saveExecutionLog(color("Running Helm command", White, Bold));
    executionLogCallback.saveExecutionLog(color(helmTemplateCommand, White, Normal));
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, String chartFile, HelmVersion helmVersion) {
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(
        HelmCommandTemplateFactory.HelmCliCommandType.RENDER_SPECIFIC_CHART_FILE, helmVersion);
    return replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, chartFile, valuesFileOptions);
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, HelmVersion helmVersion) {
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(
        HelmCommandTemplateFactory.HelmCliCommandType.RENDER_CHART, helmVersion);
    return replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, EMPTY, valuesFileOptions);
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

  public List<KubernetesResourceId> fetchAllResourcesForRelease(K8sDeleteTaskParameters k8sDeleteTaskParameters,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    String releaseName = k8sDeleteTaskParameters.getReleaseName();
    executionLogCallback.saveExecutionLog("Fetching all resources created for release: " + releaseName);

    ConfigMap configMap = kubernetesContainerService.getConfigMap(kubernetesConfig, releaseName);

    if (configMap == null || isEmpty(configMap.getData()) || isBlank(configMap.getData().get(ReleaseHistoryKeyName))) {
      executionLogCallback.saveExecutionLog("No resource history was available");
      return emptyList();
    }

    String releaseHistoryDataString = configMap.getData().get(ReleaseHistoryKeyName);
    ReleaseHistory releaseHistory = ReleaseHistory.createFromData(releaseHistoryDataString);

    if (isEmpty(releaseHistory.getReleases())) {
      return emptyList();
    }

    Map<String, KubernetesResourceId> kubernetesResourceIdMap = new HashMap<>();
    for (Release release : releaseHistory.getReleases()) {
      if (isNotEmpty(release.getResources())) {
        release.getResources().forEach(
            resource -> kubernetesResourceIdMap.put(generateResourceIdentifier(resource), resource));
      }
    }

    KubernetesResourceId harnessGeneratedCMResource = KubernetesResourceId.builder()
                                                          .kind(configMap.getKind())
                                                          .name(releaseName)
                                                          .namespace(kubernetesConfig.getNamespace())
                                                          .build();
    kubernetesResourceIdMap.put(generateResourceIdentifier(harnessGeneratedCMResource), harnessGeneratedCMResource);
    return new ArrayList<>(kubernetesResourceIdMap.values());
  }

  public List<KubernetesResourceId> getResourceIdsForDeletion(K8sDeleteTaskParameters k8sDeleteTaskParameters,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    List<KubernetesResourceId> kubernetesResourceIds =
        fetchAllResourcesForRelease(k8sDeleteTaskParameters, kubernetesConfig, executionLogCallback);

    // If namespace deletion is NOT selected,remove all Namespace resources from deletion list
    if (!k8sDeleteTaskParameters.isDeleteNamespacesForRelease()) {
      kubernetesResourceIds =
          kubernetesResourceIds.stream()
              .filter(kubernetesResourceId -> !Namespace.name().equals(kubernetesResourceId.getKind()))
              .collect(toList());
    }

    return k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(kubernetesResourceIds);
  }

  String generateResourceIdentifier(KubernetesResourceId resourceId) {
    return new StringBuilder(128)
        .append(resourceId.getNamespace())
        .append('/')
        .append(resourceId.getKind())
        .append('/')
        .append(resourceId.getName())
        .toString();
  }
}
