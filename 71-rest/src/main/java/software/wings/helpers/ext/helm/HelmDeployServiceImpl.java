package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS;
import static io.harness.k8s.manifest.ManifestHelper.getEligibleWorkloads;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replace;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.esotericsoftware.yamlbeans.YamlException;
import io.fabric8.kubernetes.api.model.Pod;
import io.harness.beans.FileData;
import io.harness.container.ContainerInfo;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitFile;
import io.harness.git.model.GitRepositoryType;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HarnessHelmDeployConfig;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.delegatetasks.helm.HelmDeployChartSpec;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmListReleasesCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.helpers.ext.helm.response.RepoListInfo;
import software.wings.helpers.ext.helm.response.SearchInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 4/1/18.
 */
@Singleton
@Slf4j
public class HelmDeployServiceImpl implements HelmDeployService {
  public static final String MANIFEST_FILE_NAME = "manifest.yaml";
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private HelmClient helmClient;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private TimeLimiter timeLimiter;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmCommandHelper helmCommandHelper;
  @Inject private DelegateLogService delegateLogService;
  @Inject private GitClient gitClient;
  @Inject private HelmTaskHelper helmTaskHelper;
  @Inject private HelmHelper helmHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private GitClientHelper gitClientHelper;
  @Inject private KubernetesContainerService kubernetesContainerService;

  private static final String ACTIVITY_ID = "ACTIVITY_ID";
  protected static final String WORKING_DIR = "./repository/helm/source/${" + ACTIVITY_ID + "}";
  public static final String FROM = " from ";
  public static final String TIMED_OUT_IN_STEADY_STATE = "Timed out waiting for controller to reach in steady state";

  @Override
  public HelmCommandResponse deploy(HelmInstallCommandRequest commandRequest) throws IOException {
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();
    HelmChartInfo helmChartInfo = null;

    try {
      HelmInstallCommandResponse commandResponse;
      executionLogCallback.saveExecutionLog(
          "List all existing deployed releases for release name: " + commandRequest.getReleaseName());

      HelmCliResponse helmCliResponse = helmClient.releaseHistory(commandRequest);
      executionLogCallback.saveExecutionLog(
          preProcessReleaseHistoryCommandOutput(helmCliResponse, commandRequest.getReleaseName()));

      fetchValuesYamlFromGitRepo(commandRequest, executionLogCallback);
      prepareRepoAndCharts(commandRequest, commandRequest.getTimeoutInMillis());

      printHelmChartKubernetesResources(commandRequest);

      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.InstallUpgrade);
      helmChartInfo = getHelmChartDetails(commandRequest);

      if (checkNewHelmInstall(commandRequest)) {
        executionLogCallback.saveExecutionLog("No previous deployment found for release. Installing chart");
        commandResponse = helmClient.install(commandRequest);
      } else {
        executionLogCallback.saveExecutionLog("Previous release exists for chart. Upgrading chart");
        commandResponse = helmClient.upgrade(commandRequest);
      }
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      commandResponse.setHelmChartInfo(helmChartInfo);

      boolean useK8sSteadyStateCheck = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
          commandRequest.isK8SteadyStateCheckEnabled(), commandRequest.isDeprecateFabric8Enabled(),
          commandRequest.getContainerServiceParams(), (ExecutionLogCallback) commandRequest.getExecutionLogCallback());
      List<KubernetesResourceId> k8sWorkloads = Collections.emptyList();
      if (useK8sSteadyStateCheck) {
        k8sWorkloads = readKubernetesResourcesIds(commandRequest, commandRequest.getVariableOverridesYamlFiles(),
            executionLogCallback, commandRequest.getTimeoutInMillis());
        ReleaseHistory releaseHistory =
            createK8sNewRelease(commandRequest, k8sWorkloads, commandRequest.getNewReleaseVersion());
        saveK8sReleaseHistory(commandRequest, commandResponse, releaseHistory);
      }

      if (commandResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return commandResponse;
      }

      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WaitForSteadyState);

      List<ContainerInfo> containerInfos = getContainerInfos(commandRequest, k8sWorkloads, useK8sSteadyStateCheck,
          executionLogCallback, commandRequest.getTimeoutInMillis());
      commandResponse.setContainerInfoList(containerInfos);

      executionLogCallback = markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WrapUp);
      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = TIMED_OUT_IN_STEADY_STATE;
      log.error(msg, e);
      executionLogCallback.saveExecutionLog(TIMED_OUT_IN_STEADY_STATE, LogLevel.ERROR);
      return HelmInstallCommandResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .output(new StringBuilder(256)
                      .append(TIMED_OUT_IN_STEADY_STATE)
                      .append(": [")
                      .append(e.getMessage())
                      .append(" ]")
                      .toString())
          .helmChartInfo(helmChartInfo)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String exceptionMessage = ExceptionUtils.getMessage(e);
      String msg = "Exception in deploying helm chart:" + exceptionMessage;
      log.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      return HelmInstallCommandResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .output(msg)
          .helmChartInfo(helmChartInfo)
          .build();
    } finally {
      if (checkDeleteReleaseNeeded(commandRequest)) {
        executionLogCallback.saveExecutionLog("Deployment failed.");
        deleteAndPurgeHelmRelease(commandRequest, executionLogCallback);
      }
      FileIo.deleteDirectoryAndItsContentIfExists(getWorkingDirectory(commandRequest));
    }
  }

  private List<ContainerInfo> getContainerInfos(HelmCommandRequest commandRequest, List<KubernetesResourceId> workloads,
      boolean useK8sSteadyStateCheck, LogCallback executionLogCallback, long timeoutInMillis) throws Exception {
    return useK8sSteadyStateCheck
        ? getKubectlContainerInfos(commandRequest, workloads, executionLogCallback, timeoutInMillis)
        : getFabric8ContainerInfos(commandRequest, executionLogCallback, timeoutInMillis);
  }

  private List<ContainerInfo> getFabric8ContainerInfos(
      HelmCommandRequest commandRequest, LogCallback executionLogCallback, long timeoutInMillis) throws Exception {
    List<ContainerInfo> containerInfos = new ArrayList<>();
    LogCallback finalExecutionLogCallback = executionLogCallback;
    timeLimiter.callWithTimeout(
        ()
            -> containerInfos.addAll(fetchContainerInfo(commandRequest, finalExecutionLogCallback, new ArrayList<>())),
        timeoutInMillis, TimeUnit.MILLISECONDS, true);
    return containerInfos;
  }

  @VisibleForTesting
  List<ContainerInfo> getKubectlContainerInfos(HelmCommandRequest commandRequest, List<KubernetesResourceId> workloads,
      LogCallback executionLogCallback, long timeoutInMillis) throws Exception {
    Kubectl client = Kubectl.client(k8sGlobalConfigService.getKubectlPath(), commandRequest.getKubeConfigLocation());

    List<ContainerInfo> containerInfoList = new ArrayList<>();
    final Map<String, List<KubernetesResourceId>> namespacewiseResources =
        workloads.stream().collect(Collectors.groupingBy(KubernetesResourceId::getNamespace));
    boolean success = true;
    for (Map.Entry<String, List<KubernetesResourceId>> entry : namespacewiseResources.entrySet()) {
      if (success) {
        final String namespace = entry.getKey();
        success = success
            && k8sTaskHelper.doStatusCheckAllResourcesForHelm(client, entry.getValue(), commandRequest.getOcPath(),
                   commandRequest.getWorkingDir(), namespace, commandRequest.getKubeConfigLocation(),
                   (ExecutionLogCallback) executionLogCallback);
        executionLogCallback.saveExecutionLog(
            format("Status check done with success [%s] for resources in namespace: [%s]", success, namespace));
        KubernetesConfig kubernetesConfig =
            containerDeploymentDelegateHelper.getKubernetesConfig(commandRequest.getContainerServiceParams());
        String releaseName = commandRequest.getReleaseName();
        List<ContainerInfo> containerInfos = commandRequest.isDeprecateFabric8Enabled()
            ? k8sTaskHelperBase.getContainerInfos(kubernetesConfig, releaseName, namespace, timeoutInMillis)
            : k8sTaskHelperBase.getContainerInfosFabric8(kubernetesConfig, releaseName, namespace, timeoutInMillis);
        containerInfoList.addAll(containerInfos);
      }
    }
    executionLogCallback.saveExecutionLog(format("Currently running Containers: [%s]", containerInfoList.size()));
    if (success) {
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return containerInfoList;
    } else {
      throw new InvalidRequestException("Steady state check failed", USER);
    }
  }

  private List<KubernetesResourceId> readKubernetesResourcesIds(HelmCommandRequest commandRequest,
      List<String> variableOverridesYamlFiles, LogCallback executionLogCallback, long timeoutInMillis)
      throws Exception {
    String workingDirPath = Paths.get(commandRequest.getWorkingDir()).normalize().toAbsolutePath().toString();

    List<FileData> manifestFiles = k8sTaskHelper.renderTemplateForHelm(
        helmClient.getHelmPath(commandRequest.getHelmVersion()), workingDirPath, variableOverridesYamlFiles,
        commandRequest.getReleaseName(), commandRequest.getContainerServiceParams().getNamespace(),
        (ExecutionLogCallback) executionLogCallback, commandRequest.getHelmVersion(), timeoutInMillis);

    List<KubernetesResource> resources =
        k8sTaskHelperBase.readManifests(manifestFiles, (ExecutionLogCallback) executionLogCallback);
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(
        resources, commandRequest.getContainerServiceParams().getNamespace());

    return getEligibleWorkloads(resources).stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());
  }

  private void prepareRepoAndCharts(HelmCommandRequest commandRequest, long timeoutInMillis) throws Exception {
    K8sDelegateManifestConfig repoConfig = commandRequest.getRepoConfig();
    if (repoConfig == null) {
      addRepoForCommand(commandRequest);
      repoUpdate(commandRequest);

      if (!helmCommandHelper.isValidChartSpecification(commandRequest.getChartSpecification())) {
        String msg = "Couldn't find valid helm chart specification from service or values.yaml from git\n"
            + ((commandRequest.getChartSpecification() != null) ? commandRequest.getChartSpecification() + "\n" : "")
            + "Please specify helm chart specification either in service or git repo\n";

        commandRequest.getExecutionLogCallback().saveExecutionLog(msg);
        throw new InvalidRequestException(msg, USER);
      }
      boolean useK8sSteadyStateCheck = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
          commandRequest.isK8SteadyStateCheckEnabled(), commandRequest.isDeprecateFabric8Enabled(),
          commandRequest.getContainerServiceParams(), (ExecutionLogCallback) commandRequest.getExecutionLogCallback());
      if (useK8sSteadyStateCheck) {
        fetchInlineChartUrl(commandRequest, timeoutInMillis);
      }
    } else {
      fetchRepo(commandRequest, timeoutInMillis);
    }
  }

  void fetchRepo(HelmCommandRequest commandRequest, long timeoutInMillis) throws Exception {
    K8sDelegateManifestConfig repoConfig = commandRequest.getRepoConfig();
    switch (repoConfig.getManifestStoreTypes()) {
      case HelmSourceRepo:
        fetchSourceRepo(commandRequest);
        break;
      case HelmChartRepo:
        fetchChartRepo(commandRequest, timeoutInMillis);
        break;
      default:
        throw new InvalidRequestException("Unsupported store type: " + repoConfig.getManifestStoreTypes(), USER);
    }
  }

  @VisibleForTesting
  void fetchInlineChartUrl(HelmCommandRequest commandRequest, long timeoutInMillis) throws Exception {
    HelmChartSpecification helmChartSpecification = commandRequest.getChartSpecification();
    String workingDirectory = Paths.get(getWorkingDirectory(commandRequest)).toString();

    helmTaskHelper.downloadChartFiles(helmChartSpecification, workingDirectory, commandRequest, timeoutInMillis);
    String chartName = isBlank(helmChartSpecification.getChartUrl())
        ? excludeRepoNameFromChartName(helmChartSpecification.getChartName())
        : helmChartSpecification.getChartName();
    commandRequest.setWorkingDir(Paths.get(workingDirectory, chartName).toString());

    commandRequest.getExecutionLogCallback().saveExecutionLog("Helm Chart Repo checked-out locally");
  }

  private String excludeRepoNameFromChartName(String chartName) {
    String[] repoNameAndChartNameSplit = chartName.split("/");
    if (repoNameAndChartNameSplit.length != 2) {
      throw new InvalidRequestException(
          "Bad chart name specified, please specify in the following format: repo/chartName");
    }

    return repoNameAndChartNameSplit[1];
  }

  @VisibleForTesting
  void fetchChartRepo(HelmCommandRequest commandRequest, long timeoutInMillis) throws Exception {
    HelmChartConfigParams helmChartConfigParams = commandRequest.getRepoConfig().getHelmChartConfigParams();
    String workingDirectory = Paths.get(getWorkingDirectory(commandRequest)).toString();

    helmTaskHelper.downloadChartFiles(helmChartConfigParams, workingDirectory, timeoutInMillis);
    commandRequest.setWorkingDir(Paths.get(workingDirectory, helmChartConfigParams.getChartName()).toString());

    commandRequest.getExecutionLogCallback().saveExecutionLog("Helm Chart Repo checked-out locally");
  }

  @VisibleForTesting
  void fetchSourceRepo(HelmCommandRequest commandRequest) {
    K8sDelegateManifestConfig sourceRepoConfig = commandRequest.getRepoConfig();
    if (sourceRepoConfig == null) {
      return;
    }
    GitConfig gitConfig = sourceRepoConfig.getGitConfig();
    GitFileConfig gitFileConfig = sourceRepoConfig.getGitFileConfig();
    gitConfig.setGitRepoType(GitRepositoryType.HELM);
    gitConfig.setBranch(gitFileConfig.getBranch());
    if (!gitFileConfig.isUseBranch()) {
      gitConfig.setReference(gitFileConfig.getCommitId());
    }

    if (isBlank(gitFileConfig.getFilePath())) {
      gitFileConfig.setFilePath(StringUtils.EMPTY);
    }

    String workingDirectory = Paths.get(getWorkingDirectory(commandRequest), gitFileConfig.getFilePath()).toString();

    encryptionService.decrypt(gitConfig, sourceRepoConfig.getEncryptedDataDetails(), false);
    gitService.downloadFiles(gitConfig, gitFileConfig, workingDirectory);

    commandRequest.setWorkingDir(workingDirectory);
    commandRequest.getExecutionLogCallback().saveExecutionLog("Repo checked-out locally");
  }

  private void printHelmChartKubernetesResources(HelmInstallCommandRequest commandRequest) {
    K8sDelegateManifestConfig repoConfig = commandRequest.getRepoConfig();
    Optional<StoreType> storeTypeOpt =
        Optional.ofNullable(repoConfig)
            .map(K8sDelegateManifestConfig::getManifestStoreTypes)
            .filter(Objects::nonNull)
            .filter(storeType -> storeType == StoreType.HelmSourceRepo || storeType == StoreType.HelmChartRepo);

    if (!storeTypeOpt.isPresent()) {
      log.warn("Unsupported store type, storeType: {}", repoConfig != null ? repoConfig.getManifestStoreTypes() : null);
      return;
    }

    String namespace = commandRequest.getNamespace();
    List<String> valueOverrides = commandRequest.getVariableOverridesYamlFiles();
    String workingDir = commandRequest.getWorkingDir();
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();

    log.debug("Printing Helm chart K8S resources, storeType: {}, namespace: {}, workingDir: {}", storeTypeOpt.get(),
        namespace, workingDir);

    try {
      List<KubernetesResource> helmKubernetesResources =
          getKubernetesResourcesFromHelmChart(commandRequest, namespace, workingDir, valueOverrides);
      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(helmKubernetesResources));
    } catch (InterruptedException e) {
      log.error("Failed to get k8s resources from Helm chart", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      String msg = format("Failed to print Helm chart manifest, location: %s", workingDir);
      log.error(msg, e);
      executionLogCallback.saveExecutionLog(msg);
    }
  }

  private List<KubernetesResource> getKubernetesResourcesFromHelmChart(
      HelmCommandRequest commandRequest, String namespace, String chartLocation, List<String> valueOverrides)
      throws InterruptedException, ExecutionException, TimeoutException, IOException {
    log.debug("Getting K8S resources from Helm chart, namespace: {}, chartLocation: {}", namespace, chartLocation);

    HelmCommandResponse commandResponse = renderHelmChart(commandRequest, namespace, chartLocation, valueOverrides);

    ManifestFile manifestFile =
        ManifestFile.builder().fileName(MANIFEST_FILE_NAME).fileContent(commandResponse.getOutput()).build();
    helmHelper.replaceManifestPlaceholdersWithLocalConfig(manifestFile);

    List<KubernetesResource> resources = ManifestHelper.processYaml(manifestFile.getFileContent());
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, namespace);

    return resources;
  }

  private LogCallback markDoneAndStartNew(
      HelmCommandRequest commandRequest, LogCallback executionLogCallback, String newName) {
    executionLogCallback.saveExecutionLog("\nDone", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    executionLogCallback = getExecutionLogCallback(commandRequest, newName);
    commandRequest.setExecutionLogCallback(executionLogCallback);
    return executionLogCallback;
  }

  private List<ContainerInfo> fetchContainerInfo(
      HelmCommandRequest commandRequest, LogCallback executionLogCallback, List<Pod> existingPods) {
    ContainerServiceParams containerServiceParams = commandRequest.getContainerServiceParams();

    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams);

    return containerDeploymentDelegateBaseHelper.getContainerInfosWhenReadyByLabels(kubernetesConfig,
        executionLogCallback, ImmutableMap.of("release", commandRequest.getReleaseName()), existingPods);
  }

  private ReleaseHistory fetchK8sReleaseHistory(HelmCommandRequest request, KubernetesConfig kubernetesConfig)
      throws IOException {
    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryFromSecret(
        kubernetesConfig, request.getReleaseName(), request.isDeprecateFabric8Enabled());

    return (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                     : ReleaseHistory.createFromData(releaseHistoryData);
  }

  private ReleaseHistory createK8sNewRelease(
      HelmCommandRequest request, List<KubernetesResourceId> resourceList, Integer releaseVersion) throws IOException {
    KubernetesConfig kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(request.getContainerServiceParams());
    ReleaseHistory releaseHistory = fetchK8sReleaseHistory(request, kubernetesConfig);
    // Need to keep only latest successful releases, older releases can be removed
    releaseHistory.cleanup();
    releaseHistory.createNewRelease(resourceList);
    if (releaseVersion != null) {
      releaseHistory.setReleaseNumber(releaseVersion);
    }

    return releaseHistory;
  }

  private void saveK8sReleaseHistory(
      HelmCommandRequest request, HelmCommandResponse response, ReleaseHistory releaseHistory) throws YamlException {
    KubernetesConfig kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(request.getContainerServiceParams());
    releaseHistory.setReleaseStatus(
        CommandExecutionStatus.SUCCESS == response.getCommandExecutionStatus() ? Status.Succeeded : Status.Failed);
    k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, request.getReleaseName(), releaseHistory.getAsYaml(), true,
        request.isDeprecateFabric8Enabled());
  }

  @Override
  public HelmCommandResponse rollback(HelmRollbackCommandRequest commandRequest) {
    LogCallback executionLogCallback = getExecutionLogCallback(commandRequest, HelmDummyCommandUnit.Rollback);
    commandRequest.setExecutionLogCallback(executionLogCallback);

    try {
      HelmInstallCommandResponse commandResponse = helmClient.rollback(commandRequest);
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      if (commandResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return commandResponse;
      }

      List<KubernetesResourceId> k8sRollbackWorkloads = Collections.emptyList();
      boolean useK8sSteadyStateCheck = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
          commandRequest.isK8SteadyStateCheckEnabled(), commandRequest.isDeprecateFabric8Enabled(),
          commandRequest.getContainerServiceParams(), (ExecutionLogCallback) executionLogCallback);

      if (useK8sSteadyStateCheck) {
        prepareWorkingDirectoryForK8sRollout(commandRequest);
        k8sRollbackWorkloads = getKubernetesResourcesIdsForRollback(commandRequest);
        ReleaseHistory releaseHistory = createK8sNewRelease(commandRequest, k8sRollbackWorkloads, null);
        saveK8sReleaseHistory(commandRequest, commandResponse, releaseHistory);
      }

      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WaitForSteadyState);

      List<ContainerInfo> containerInfos = getContainerInfos(commandRequest, k8sRollbackWorkloads,
          useK8sSteadyStateCheck, executionLogCallback, commandRequest.getTimeoutInMillis());
      commandResponse.setContainerInfoList(containerInfos);

      executionLogCallback.saveExecutionLog("\nDone", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = TIMED_OUT_IN_STEADY_STATE;
      log.error(msg, e);
      executionLogCallback.saveExecutionLog(TIMED_OUT_IN_STEADY_STATE, LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, ExceptionUtils.getMessage(e));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      log.error("Helm chart rollback failed [{}]", commandRequest.toString(), e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, ExceptionUtils.getMessage(e));
    } finally {
      cleanupWorkingDirectory(commandRequest);
    }
  }

  private void prepareWorkingDirectoryForK8sRollout(HelmCommandRequest commandRequest) throws IOException {
    String workingDirectory = getWorkingDirectory(commandRequest);
    FileIo.createDirectoryIfDoesNotExist(workingDirectory);
    commandRequest.setWorkingDir(workingDirectory);
  }

  private void cleanupWorkingDirectory(HelmCommandRequest commandRequest) {
    try {
      if (commandRequest.getWorkingDir() != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(commandRequest.getWorkingDir());
      }
    } catch (IOException e) {
      log.info("Unable to delete working directory: " + commandRequest.getWorkingDir(), e);
    }
  }

  private List<KubernetesResourceId> getKubernetesResourcesIdsForRollback(HelmRollbackCommandRequest request)
      throws IOException {
    KubernetesConfig kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(request.getContainerServiceParams());
    ReleaseHistory releaseHistory = fetchK8sReleaseHistory(request, kubernetesConfig);
    Release rollbackRelease = releaseHistory.getRelease(request.getPrevReleaseVersion());
    notNullCheck("Unable to find release " + request.getPrevReleaseVersion(), rollbackRelease);

    if (Status.Succeeded != rollbackRelease.getStatus()) {
      throw new InvalidRequestException("Invalid status for release with number " + request.getPrevReleaseVersion()
              + ". Expected 'Succeeded' status, actual status is '" + rollbackRelease.getStatus() + "'",
          USER);
    }

    return rollbackRelease.getResources();
  }

  @Override
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        HelmCliResponse cliResponse = helmClient.getClientAndServerVersion(helmCommandRequest);
        if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
          throw new InvalidRequestException(cliResponse.getOutput());
        }

        boolean helm3 = isHelm3(cliResponse.getOutput());
        CommandExecutionStatus commandExecutionStatus =
            helm3 ? CommandExecutionStatus.FAILURE : CommandExecutionStatus.SUCCESS;
        return new HelmCommandResponse(commandExecutionStatus, cliResponse.getOutput());
      }, DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out while finding helm client and server version";
      log.error(msg, e);
      throw new InvalidRequestException(msg);
    } catch (Exception e) {
      throw new InvalidRequestException("Some error occurred while finding Helm client and server version", e);
    }
  }

  @Override
  public HelmCommandResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();

    executionLogCallback.saveExecutionLog(
        "Checking if the repository has already been added", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCliResponse cliResponse = helmClient.getHelmRepoList(commandRequest);
    if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      throw new InvalidRequestException(cliResponse.getOutput());
    }

    List<RepoListInfo> repoListInfos = parseHelmAddRepoOutput(cliResponse.getOutput());

    boolean repoAlreadyAdded = repoListInfos.stream().anyMatch(
        repoListInfo -> repoListInfo.getRepoUrl().equals(commandRequest.getChartSpecification().getChartUrl()));

    String responseMsg;
    if (!repoAlreadyAdded) {
      executionLogCallback.saveExecutionLog("Repository not found", LogLevel.INFO, CommandExecutionStatus.RUNNING);
      executionLogCallback.saveExecutionLog("Adding repository " + commandRequest.getChartSpecification().getChartUrl()
              + " with name " + commandRequest.getRepoName(),
          LogLevel.INFO, CommandExecutionStatus.RUNNING);
      cliResponse = helmClient.addPublicRepo(commandRequest);
      if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
        String msg = "Failed to add repository. Reason: " + cliResponse.getOutput();
        executionLogCallback.saveExecutionLog(msg);
        throw new InvalidRequestException(msg);
      }
      responseMsg = "Successfully added repository " + commandRequest.getChartSpecification().getChartUrl()
          + " with name " + commandRequest.getRepoName() + "\n";
    } else {
      responseMsg = "Repo " + commandRequest.getChartSpecification().getChartUrl() + " already added. Ignore adding\n";
    }

    return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), responseMsg);
  }

  @Override
  public HelmCommandResponse renderHelmChart(HelmCommandRequest commandRequest, String namespace, String chartLocation,
      List<String> valueOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException {
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();

    log.debug("Rendering Helm chart, namespace: {}, chartLocation: {}", namespace, chartLocation);

    executionLogCallback.saveExecutionLog("Rendering Helm chart", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCliResponse cliResponse = helmClient.renderChart(commandRequest, chartLocation, namespace, valueOverrides);
    if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      String msg = format("Failed to render chart location: %s. Reason %s ", chartLocation, cliResponse.getOutput());
      executionLogCallback.saveExecutionLog(msg);
      throw new InvalidRequestException(msg);
    }

    return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), cliResponse.getOutput());
  }

  @Override
  public HelmCommandResponse ensureHelm3Installed(HelmCommandRequest commandRequest) {
    String helmPath = k8sGlobalConfigService.getHelmPath(HelmVersion.V3);
    if (isNotBlank(helmPath)) {
      return new HelmCommandResponse(CommandExecutionStatus.SUCCESS, format("Helm3 is installed at [%s]", helmPath));
    }
    return new HelmCommandResponse(CommandExecutionStatus.FAILURE, "Helm3 not installed in the delegate client tools");
  }

  @Override
  public HelmCommandResponse ensureHelmInstalled(HelmCommandRequest commandRequest) {
    if (commandRequest.getHelmVersion() == null) {
      log.error("Did not expect null value of helmVersion, defaulting to V2");
    }
    return commandRequest.getHelmVersion() == HelmVersion.V3 ? ensureHelm3Installed(commandRequest)
                                                             : ensureHelmCliAndTillerInstalled(commandRequest);
  }

  boolean isHelm3(String cliResponse) {
    return isNotEmpty(cliResponse) && cliResponse.toLowerCase().startsWith("v3.");
  }

  @Override
  public HelmListReleasesCommandResponse listReleases(HelmInstallCommandRequest helmCommandRequest) {
    try {
      HelmCliResponse helmCliResponse = helmClient.listReleases(helmCommandRequest);
      List<ReleaseInfo> releaseInfoList =
          parseHelmReleaseCommandOutput(helmCliResponse.getOutput(), HelmCommandType.LIST_RELEASE);
      return HelmListReleasesCommandResponse.builder()
          .commandExecutionStatus(helmCliResponse.getCommandExecutionStatus())
          .output(helmCliResponse.getOutput())
          .releaseInfoList(releaseInfoList)
          .build();
    } catch (Exception e) {
      log.error("Helm list releases failed", e);
      return HelmListReleasesCommandResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .output(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @Override
  public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest) {
    List<ReleaseInfo> releaseInfoList = new ArrayList<>();
    try {
      HelmCliResponse helmCliResponse = helmClient.releaseHistory(helmCommandRequest);
      releaseInfoList =
          parseHelmReleaseCommandOutput(helmCliResponse.getOutput(), helmCommandRequest.getHelmCommandType());
    } catch (Exception e) {
      log.error("Helm list releases failed", e);
    }
    return HelmReleaseHistoryCommandResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .releaseInfoList(releaseInfoList)
        .build();
  }

  private List<ReleaseInfo> parseHelmReleaseCommandOutput(String listReleaseOutput, HelmCommandType helmCommandType)
      throws IOException {
    if (isEmpty(listReleaseOutput)) {
      return new ArrayList<>();
    }
    CSVFormat csvFormat = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter('\t').withTrim();
    return CSVParser.parse(listReleaseOutput, csvFormat)
        .getRecords()
        .stream()
        .map(helmCommandType == HelmCommandType.RELEASE_HISTORY ? this ::releaseHistoryCsvRecordToReleaseInfo
                                                                : this ::listReleaseCsvRecordToReleaseInfo)
        .collect(Collectors.toList());
  }

  private ReleaseInfo listReleaseCsvRecordToReleaseInfo(CSVRecord releaseRecord) {
    return ReleaseInfo.builder()
        .name(releaseRecord.get("NAME"))
        .revision(releaseRecord.get("REVISION"))
        .status(releaseRecord.get("STATUS"))
        .chart(releaseRecord.get("CHART"))
        .namespace(releaseRecord.get("NAMESPACE"))
        .build();
  }

  private ReleaseInfo releaseHistoryCsvRecordToReleaseInfo(CSVRecord releaseRecord) {
    return ReleaseInfo.builder()
        .revision(releaseRecord.get("REVISION"))
        .status(releaseRecord.get("STATUS"))
        .chart(releaseRecord.get("CHART"))
        .build();
  }

  private List<RepoListInfo> parseHelmAddRepoOutput(String listReleaseOutput) throws IOException {
    if (isEmpty(listReleaseOutput)) {
      return new ArrayList<>();
    }

    CSVFormat csvFormat = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter('\t').withTrim();
    return CSVParser.parse(listReleaseOutput, csvFormat)
        .getRecords()
        .stream()
        .map(this ::repoListCsvRecordToRepoListInfo)
        .collect(Collectors.toList());
  }

  private RepoListInfo repoListCsvRecordToRepoListInfo(CSVRecord repoListRecord) {
    return RepoListInfo.builder().repoName(repoListRecord.get("NAME")).repoUrl(repoListRecord.get("URL")).build();
  }

  private String preProcessReleaseHistoryCommandOutput(HelmCliResponse helmCliResponse, String releaseName) {
    if (helmCliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      return "Release: \"" + releaseName + "\" not found\n";
    }

    return helmCliResponse.getOutput();
  }

  void deleteAndPurgeHelmRelease(HelmInstallCommandRequest commandRequest, LogCallback executionLogCallback) {
    try {
      String message = "Cleaning up. Deleting the release, freeing it up for later use";
      executionLogCallback.saveExecutionLog(message);

      HelmCliResponse deleteCommandResponse = helmClient.deleteHelmRelease(commandRequest);
      executionLogCallback.saveExecutionLog(deleteCommandResponse.getOutput());
    } catch (Exception e) {
      log.error("Helm delete failed", e);
    }
  }

  private boolean checkNewHelmInstall(HelmInstallCommandRequest commandRequest) {
    HelmListReleasesCommandResponse commandResponse = listReleases(commandRequest);

    log.info(commandResponse.getOutput());
    return commandResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        && isEmpty(commandResponse.getReleaseInfoList());
  }

  private boolean checkDeleteReleaseNeeded(HelmInstallCommandRequest commandRequest) {
    HelmListReleasesCommandResponse commandResponse = listReleases(commandRequest);

    log.info(commandResponse.getOutput());
    if (commandResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      if (isEmpty(commandResponse.getReleaseInfoList())) {
        return false;
      }

      return commandResponse.getReleaseInfoList().stream().anyMatch(releaseInfo
          -> releaseInfo.getRevision().equals("1") && isFailedStatus(releaseInfo.getStatus())
              && releaseInfo.getName().equals(commandRequest.getReleaseName()));
    }

    return false;
  }

  boolean isFailedStatus(String status) {
    return status.equalsIgnoreCase("failed");
  }

  private void fetchValuesYamlFromGitRepo(HelmCommandRequest commandRequest, LogCallback executionLogCallback) {
    GitConfig gitConfig = commandRequest.getGitConfig();
    if (gitConfig == null) {
      return;
    }

    try {
      encryptionService.decrypt(gitConfig, commandRequest.getEncryptedDataDetails(), false);
      GitFileConfig gitFileConfig = commandRequest.getGitFileConfig();
      String repoUrl = gitConfig.getRepoUrl();

      String msg = "Fetching values yaml files from git:\n"
          + "Git repo: " + repoUrl + "\n"
          + (isNotBlank(gitFileConfig.getBranch()) ? ("Branch: " + gitFileConfig.getBranch() + "\n") : "")
          + (isNotBlank(gitFileConfig.getCommitId()) ? ("Commit Id: " + gitFileConfig.getCommitId() + "\n") : "")
          + "File path: " + gitFileConfig.getFilePath() + "\n";
      executionLogCallback.saveExecutionLog(msg);
      log.info(msg);

      GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, gitFileConfig.getConnectorId(),
          gitFileConfig.getCommitId(), gitFileConfig.getBranch(),
          Collections.singletonList(gitFileConfig.getFilePath()), gitFileConfig.isUseBranch());

      if (isNotEmpty(gitFetchFilesResult.getFiles())) {
        executionLogCallback.saveExecutionLog(
            "Found " + gitFetchFilesResult.getFiles().size() + " value yaml files from git\n");

        List<String> valuesYamlFilesFromGit = new ArrayList<>();

        for (GitFile gitFile : gitFetchFilesResult.getFiles()) {
          if (isNotBlank(gitFile.getFileContent())) {
            valuesYamlFilesFromGit.add(gitFile.getFileContent());
            boolean valueOverrriden = false;

            Optional<HarnessHelmDeployConfig> optionalHarnessHelmDeployConfig =
                helmCommandHelper.generateHelmDeployChartSpecFromYaml(gitFile.getFileContent());
            if (optionalHarnessHelmDeployConfig.isPresent()) {
              HelmDeployChartSpec helmDeployChartSpec = optionalHarnessHelmDeployConfig.get().getHelmDeployChartSpec();

              HelmChartSpecification helmChartSpecification;
              if (commandRequest.getChartSpecification() == null) {
                helmChartSpecification = HelmChartSpecification.builder().build();
              } else {
                helmChartSpecification = commandRequest.getChartSpecification();
              }

              if (isNotBlank(helmDeployChartSpec.getName())) {
                String chartNameMsg = isNotBlank(helmChartSpecification.getChartName())
                    ? FROM + helmChartSpecification.getChartName()
                    : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart name" + chartNameMsg + " to " + helmDeployChartSpec.getName());
                helmChartSpecification.setChartName(helmDeployChartSpec.getName());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getUrl())) {
                String chartUrlMsg =
                    isNotBlank(helmChartSpecification.getChartUrl()) ? FROM + helmChartSpecification.getChartUrl() : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart url" + chartUrlMsg + " to " + helmDeployChartSpec.getUrl());
                helmChartSpecification.setChartUrl(helmDeployChartSpec.getUrl());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getVersion())) {
                String chartVersionMsg = isNotBlank(helmChartSpecification.getChartVersion())
                    ? FROM + helmChartSpecification.getChartVersion()
                    : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart version" + chartVersionMsg + " to " + helmDeployChartSpec.getVersion());
                helmChartSpecification.setChartVersion(helmDeployChartSpec.getVersion());
                valueOverrriden = true;
              }

              if (valueOverrriden) {
                commandRequest.setChartSpecification(helmChartSpecification);
                executionLogCallback.saveExecutionLog("");
              }
            }
          }
        }

        if (isNotEmpty(valuesYamlFilesFromGit)) {
          if (isEmpty(commandRequest.getVariableOverridesYamlFiles())) {
            commandRequest.setVariableOverridesYamlFiles(valuesYamlFilesFromGit);
          } else {
            List<String> variableOverridesYamlFiles = new ArrayList<>();
            variableOverridesYamlFiles.addAll(commandRequest.getVariableOverridesYamlFiles());
            variableOverridesYamlFiles.addAll(valuesYamlFilesFromGit);
            commandRequest.setVariableOverridesYamlFiles(variableOverridesYamlFiles);
          }
        }
      } else {
        executionLogCallback.saveExecutionLog("No values yaml file found on git");
      }
    } catch (Exception ex) {
      String msg = "Exception in adding values yaml from git. " + ExceptionUtils.getMessage(ex);
      log.error(msg);
      executionLogCallback.saveExecutionLog(msg);
      throw ex;
    }
  }

  private void addRepoForCommand(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, IOException, TimeoutException {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    if (helmCommandRequest.getHelmCommandType() != HelmCommandType.INSTALL) {
      return;
    }

    if (helmCommandRequest.getChartSpecification() != null
        && isNotEmpty(helmCommandRequest.getChartSpecification().getChartUrl())
        && isNotEmpty(helmCommandRequest.getRepoName())) {
      executionLogCallback.saveExecutionLog(
          "Adding helm repository " + helmCommandRequest.getChartSpecification().getChartUrl(), LogLevel.INFO,
          CommandExecutionStatus.RUNNING);
      HelmCommandResponse helmCommandResponse = addPublicRepo(helmCommandRequest);
      executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
    }
  }

  private void repoUpdate(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, TimeoutException, IOException {
    if (HelmCommandType.INSTALL != helmCommandRequest.getHelmCommandType()) {
      return;
    }

    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();
    executionLogCallback.saveExecutionLog("Updating information about charts from the respective chart repositories");

    try {
      HelmCliResponse helmCliResponse = helmClient.repoUpdate(helmCommandRequest);
      executionLogCallback.saveExecutionLog(helmCliResponse.getOutput());
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(
          "Failed to update information about charts with message " + ExceptionUtils.getMessage(ex));
      throw ex;
    }
  }

  protected LogCallback getExecutionLogCallback(HelmCommandRequest helmCommandRequest, String name) {
    return new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(),
        helmCommandRequest.getAppId(), helmCommandRequest.getActivityId(), name);
  }

  public String getWorkingDirectory(HelmCommandRequest commandRequest) {
    return replace(WORKING_DIR, "${" + ACTIVITY_ID + "}", commandRequest.getActivityId());
  }

  private HelmChartInfo getHelmChartDetails(HelmInstallCommandRequest request) {
    K8sDelegateManifestConfig repoConfig = request.getRepoConfig();
    HelmChartInfo helmChartInfo = null;

    try {
      if (repoConfig == null) {
        helmChartInfo = getHelmChartInfoFromChartSpec(request);
      } else {
        switch (repoConfig.getManifestStoreTypes()) {
          case HelmSourceRepo:
            helmChartInfo = helmTaskHelper.getHelmChartInfoFromChartsYamlFile(request);
            helmChartInfo.setRepoUrl(repoConfig.getGitConfig().getRepoUrl());
            break;

          case HelmChartRepo:
            helmChartInfo = helmTaskHelper.getHelmChartInfoFromChartsYamlFile(request);
            helmChartInfo.setRepoUrl(
                helmHelper.getRepoUrlForHelmRepoConfig(request.getRepoConfig().getHelmChartConfigParams()));
            break;

          default:
            log.warn("Unsupported store type: " + repoConfig.getManifestStoreTypes());
        }
      }
    } catch (Exception ex) {
      log.info("Exception while getting helm chart info ", ex);
    }

    return helmChartInfo;
  }

  private String getChartInfoForSpecWithRepoUrl(HelmInstallCommandRequest request) throws Exception {
    if (isNotBlank(request.getChartSpecification().getChartVersion())) {
      return request.getChartSpecification().getChartName();
    }

    HelmCliResponse cliResponse = helmClient.getHelmRepoList(request);
    if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      return null;
    }

    List<RepoListInfo> repoListInfos = parseHelmAddRepoOutput(cliResponse.getOutput());
    Optional<RepoListInfo> repoListInfo =
        repoListInfos.stream()
            .filter(repoListInfoObject
                -> repoListInfoObject.getRepoUrl().equals(request.getChartSpecification().getChartUrl()))
            .findFirst();

    if (!repoListInfo.isPresent()) {
      return null;
    }

    return repoListInfo.get().getRepoName() + "/" + request.getChartSpecification().getChartName();
  }

  private String getChartVersion(HelmInstallCommandRequest request, String chartInfo) throws Exception {
    HelmChartSpecification chartSpecification = request.getChartSpecification();

    if (isNotBlank(chartSpecification.getChartVersion())) {
      return chartSpecification.getChartVersion();
    }

    HelmCliResponse helmCliResponse = helmClient.searchChart(request, chartInfo);
    List<SearchInfo> searchInfos = parseHelmSearchCommandOutput(helmCliResponse.getOutput());

    if (isEmpty(searchInfos)) {
      return null;
    }
    SearchInfo searchInfo = searchInfos.get(0);
    return searchInfo.getChartVersion();
  }

  private String getChartName(String chartInfo) {
    int index = chartInfo.indexOf('/');
    if (index == -1) {
      return chartInfo;
    }

    return chartInfo.substring(chartInfo.indexOf('/') + 1);
  }

  private String getChartUrl(String url, String chartInfo) {
    if (isNotBlank(url)) {
      return url;
    }

    int index = chartInfo.indexOf('/');
    if (index == -1) {
      return null;
    }

    return chartInfo.substring(0, chartInfo.indexOf('/'));
  }

  private HelmChartInfo getHelmChartInfoFromChartSpec(HelmInstallCommandRequest request) throws Exception {
    String chartInfo;
    HelmChartSpecification chartSpecification = request.getChartSpecification();

    if (isBlank(chartSpecification.getChartUrl())) {
      chartInfo = chartSpecification.getChartName();
    } else {
      chartInfo = getChartInfoForSpecWithRepoUrl(request);
    }

    if (chartInfo == null || isBlank(chartInfo)) {
      return null;
    }

    return HelmChartInfo.builder()
        .name(getChartName(chartInfo))
        .version(getChartVersion(request, chartInfo))
        .repoUrl(getChartUrl(chartSpecification.getChartUrl(), chartInfo))
        .build();
  }

  private List<SearchInfo> parseHelmSearchCommandOutput(String searchOutput) throws IOException {
    if (isEmpty(searchOutput)) {
      return new ArrayList<>();
    }

    CSVFormat csvFormat = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter('\t').withTrim();
    return CSVParser.parse(searchOutput, csvFormat)
        .getRecords()
        .stream()
        .map(this ::convertSearchCsvRecordToSearchInfo)
        .collect(Collectors.toList());
  }

  private SearchInfo convertSearchCsvRecordToSearchInfo(CSVRecord releaseRecord) {
    return SearchInfo.builder()
        .name(releaseRecord.get("NAME"))
        .chartVersion(releaseRecord.get("CHART VERSION"))
        .appVersion(releaseRecord.get("APP VERSION"))
        .build();
  }
}
