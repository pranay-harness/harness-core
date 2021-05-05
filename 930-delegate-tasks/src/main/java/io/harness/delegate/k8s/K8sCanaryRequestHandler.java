package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sCanaryHandlerConfig;
import io.harness.delegate.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.k8s.exception.KubernetesExceptionMessage;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.exception.K8sCanaryDataException;
import io.harness.delegate.task.k8s.exception.K8sCanaryDataException.K8sCanaryDataExceptionBuilder;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.esotericsoftware.yamlbeans.YamlException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@OwnedBy(CDP)
public class K8sCanaryRequestHandler extends K8sRequestHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private K8sCanaryBaseHandler k8sCanaryBaseHandler;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  private final K8sCanaryHandlerConfig k8sCanaryHandlerConfig = new K8sCanaryHandlerConfig();
  private boolean canaryWorkloadDeployed;
  private boolean shouldSaveReleaseHistory;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sCanaryDeployRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sDeployRequest", "Must be instance of K8sCanaryDeployRequestK8sCanaryDeployRequest"));
    }

    K8sCanaryDeployRequest k8sCanaryDeployRequest = (K8sCanaryDeployRequest) k8sDeployRequest;
    k8sCanaryHandlerConfig.setReleaseName(k8sCanaryDeployRequest.getReleaseName());
    k8sCanaryHandlerConfig.setManifestFilesDirectory(
        Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString());
    final long timeoutInMillis = getTimeoutMillisFromMinutes(k8sCanaryDeployRequest.getTimeoutIntervalInMin());

    List<String> manifestHelperFiles = isEmpty(k8sCanaryDeployRequest.getValuesYamlList())
        ? k8sCanaryDeployRequest.getOpenshiftParamList()
        : k8sCanaryDeployRequest.getValuesYamlList();

    startNewCommandUnit(FetchFiles, k8sCanaryDeployRequest.isShouldOpenFetchFilesLogStream());
    boolean success = k8sTaskHelperBase.fetchManifestFilesAndWriteToDirectory(
        k8sCanaryDeployRequest.getManifestDelegateConfig(), k8sCanaryHandlerConfig.getManifestFilesDirectory(),
        getCurrentLogCallback(), timeoutInMillis, k8sCanaryDeployRequest.getAccountId(), false);
    if (!success) {
      throw new ExplanationException(KubernetesExceptionExplanation.FETCH_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.FETCH_MANIFEST_FAILED));
    }

    startNewCommandUnit(Init, true);
    init(k8sCanaryDeployRequest, k8sDelegateTaskParams, getCurrentLogCallback());

    startNewCommandUnit(Prepare, true);
    prepareForCanary(k8sCanaryDeployRequest, k8sDelegateTaskParams, getCurrentLogCallback());

    startNewCommandUnit(Apply, true);
    shouldSaveReleaseHistory = true;
    success = k8sTaskHelperBase.applyManifests(k8sCanaryHandlerConfig.getClient(),
        k8sCanaryHandlerConfig.getResources(), k8sDelegateTaskParams, getCurrentLogCallback(), true, false);
    if (!success) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.APPLY_MANIFEST_FAILED,
          KubernetesExceptionExplanation.APPLY_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.APPLY_MANIFEST_FAILED));
    }

    // At this point we're sure that manifest has been applied successfully and canary workload is deployed
    this.canaryWorkloadDeployed = true;
    startNewCommandUnit(WaitForSteadyState, true);
    KubernetesResource canaryWorkload = k8sCanaryHandlerConfig.getCanaryWorkload();
    success = k8sTaskHelperBase.doStatusCheck(k8sCanaryHandlerConfig.getClient(), canaryWorkload.getResourceId(),
        k8sDelegateTaskParams, getCurrentLogCallback(), false);
    if (!success) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED,
          KubernetesExceptionExplanation.WAIT_FOR_STEADY_STATE_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.WAIT_FOR_STEADY_STATE_FAILED));
    }

    startNewCommandUnit(WrapUp, true);
    List<K8sPod> allPods = k8sCanaryBaseHandler.getAllPods(
        k8sCanaryHandlerConfig, k8sCanaryDeployRequest.getReleaseName(), timeoutInMillis);
    k8sCanaryBaseHandler.wrapUp(k8sCanaryHandlerConfig.getClient(), k8sDelegateTaskParams, getCurrentLogCallback());

    ReleaseHistory releaseHistory = k8sCanaryHandlerConfig.getReleaseHistory();
    releaseHistory.setReleaseStatus(Release.Status.Succeeded);
    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(k8sCanaryHandlerConfig.getKubernetesConfig(),
        k8sCanaryDeployRequest.getReleaseName(), releaseHistory.getAsYaml());
    getCurrentLogCallback().saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(K8sCanaryDeployResponse.builder()
                               .canaryWorkload(canaryWorkload.getResourceId().namespaceKindNameRef())
                               .k8sPodList(allPods)
                               .releaseNumber(k8sCanaryHandlerConfig.getCurrentRelease().getNumber())
                               .currentInstances(k8sCanaryHandlerConfig.getTargetInstances())
                               .canaryWorkloadDeployed(this.canaryWorkloadDeployed)
                               .build())
        .build();
  }

  @Override
  public boolean isErrorFrameworkSupported() {
    return true;
  }

  @VisibleForTesting
  void init(K8sCanaryDeployRequest request, K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback logCallback)
      throws Exception {
    logCallback.saveExecutionLog("Initializing..\n");
    k8sCanaryHandlerConfig.setKubernetesConfig(
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(request.getK8sInfraDelegateConfig()));
    k8sCanaryHandlerConfig.setClient(
        Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath()));

    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(
        k8sCanaryHandlerConfig.getKubernetesConfig(), request.getReleaseName());

    ReleaseHistory releaseHistory = (StringUtils.isEmpty(releaseHistoryData))
        ? ReleaseHistory.createNew()
        : ReleaseHistory.createFromData(releaseHistoryData);
    k8sCanaryHandlerConfig.setReleaseHistory(releaseHistory);

    k8sTaskHelperBase.deleteSkippedManifestFiles(k8sCanaryHandlerConfig.getManifestFilesDirectory(), logCallback);
    List<String> manifestHelperFiles =
        isEmpty(request.getValuesYamlList()) ? request.getOpenshiftParamList() : request.getValuesYamlList();
    List<FileData> manifestFiles = k8sTaskHelperBase.renderTemplate(k8sDelegateTaskParams,
        request.getManifestDelegateConfig(), k8sCanaryHandlerConfig.getManifestFilesDirectory(), manifestHelperFiles,
        request.getReleaseName(), k8sCanaryHandlerConfig.getKubernetesConfig().getNamespace(), logCallback,
        request.getTimeoutIntervalInMin());

    List<KubernetesResource> resources = k8sTaskHelperBase.readManifests(manifestFiles, logCallback);
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(
        resources, k8sCanaryHandlerConfig.getKubernetesConfig().getNamespace());

    k8sCanaryBaseHandler.updateDestinationRuleManifestFilesWithSubsets(
        resources, k8sCanaryHandlerConfig.getKubernetesConfig(), logCallback);
    k8sCanaryBaseHandler.updateVirtualServiceManifestFilesWithRoutes(
        resources, k8sCanaryHandlerConfig.getKubernetesConfig(), logCallback);
    k8sCanaryHandlerConfig.setResources(resources);

    logCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));
    logCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(k8sCanaryHandlerConfig.getResources()));

    if (request.isSkipDryRun()) {
      logCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
      logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return;
    }

    if (!k8sTaskHelperBase.dryRunManifests(k8sCanaryHandlerConfig.getClient(), k8sCanaryHandlerConfig.getResources(),
            k8sDelegateTaskParams, logCallback)) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.DRY_RUN_MANIFEST_FAILED,
          KubernetesExceptionExplanation.DRY_RUN_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.DRY_RUN_MANIFEST_FAILED));
    }
  }

  @VisibleForTesting
  void prepareForCanary(K8sCanaryDeployRequest k8sCanaryDeployRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback logCallback) throws Exception {
    k8sCanaryBaseHandler.prepareForCanary(k8sCanaryHandlerConfig, k8sDelegateTaskParams,
        k8sCanaryDeployRequest.isSkipResourceVersioning(), logCallback, true);

    Integer currentInstances =
        k8sCanaryBaseHandler.getCurrentInstances(k8sCanaryHandlerConfig, k8sDelegateTaskParams, logCallback);
    Integer targetInstances = currentInstances;

    switch (k8sCanaryDeployRequest.getInstanceUnitType()) {
      case COUNT:
        targetInstances = k8sCanaryDeployRequest.getInstances();
        break;

      case PERCENTAGE:
        Integer maxInstances;
        if (k8sCanaryDeployRequest.getMaxInstances() != null) {
          maxInstances = k8sCanaryDeployRequest.getMaxInstances();
        } else {
          maxInstances = currentInstances;
        }
        targetInstances = k8sTaskHelperBase.getTargetInstancesForCanary(
            k8sCanaryDeployRequest.getInstances(), maxInstances, logCallback);
        break;

      default:
        unhandled(k8sCanaryDeployRequest.getInstanceUnitType());
    }

    k8sCanaryBaseHandler.updateTargetInstances(k8sCanaryHandlerConfig, targetInstances, logCallback);

    logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  @Override
  protected void onTaskFailed(K8sDeployRequest deployRequest) {
    if (shouldSaveReleaseHistory) {
      K8sCanaryDeployRequest k8sCanaryDeployRequest = (K8sCanaryDeployRequest) deployRequest;
      try {
        k8sCanaryBaseHandler.failAndSaveKubernetesRelease(
            k8sCanaryHandlerConfig, k8sCanaryDeployRequest.getReleaseName());
      } catch (YamlException ex) {
        log.error("Failed to save release history", ex);
        throw new KubernetesTaskException(
            KubernetesExceptionMessage.RELEASE_HISTORY_YAML_EXCEPTION + ". " + ex.getMessage());
      }
    }
  }

  @Override
  protected Exception handleException(Exception exception) {
    K8sCanaryDataExceptionBuilder canaryDataExceptionBuilder = K8sCanaryDataException.dataBuilder();
    KubernetesResource canaryWorkload = k8sCanaryHandlerConfig.getCanaryWorkload();

    if (canaryWorkload != null && canaryWorkload.getResourceId() != null) {
      canaryDataExceptionBuilder.canaryWorkload(canaryWorkload.getResourceId().namespaceKindNameRef());
    }

    canaryDataExceptionBuilder.canaryWorkloadDeployed(this.canaryWorkloadDeployed);
    canaryDataExceptionBuilder.cause(exception);
    return canaryDataExceptionBuilder.build();
  }

  @VisibleForTesting
  K8sCanaryHandlerConfig getK8sCanaryHandlerConfig() {
    return k8sCanaryHandlerConfig;
  }
}
