package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.k8s.exception.KubernetesExceptionMessage;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.esotericsoftware.yamlbeans.YamlException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@NoArgsConstructor
@Slf4j
public class K8sRollingRequestHandler extends K8sRequestHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject K8sRollingBaseHandler k8sRollingBaseHandler;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  List<KubernetesResource> managedWorkloads;
  List<KubernetesResource> resources;
  private String releaseName;
  private String manifestFilesDirectory;

  private boolean shouldSaveReleaseHistory;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sRollingDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sRollingDeployRequest"));
    }

    K8sRollingDeployRequest k8sRollingDeployRequest = (K8sRollingDeployRequest) k8sDeployRequest;

    releaseName = k8sRollingDeployRequest.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sDeployRequest.getTimeoutIntervalInMin());

    startNewCommandUnit(FetchFiles, k8sRollingDeployRequest.isShouldOpenFetchFilesLogStream());
    boolean success = k8sTaskHelperBase.fetchManifestFilesAndWriteToDirectory(
        k8sRollingDeployRequest.getManifestDelegateConfig(), manifestFilesDirectory, getCurrentLogCallback(),
        steadyStateTimeoutInMillis, k8sRollingDeployRequest.getAccountId(), false);
    if (!success) {
      // Don't expect to this to happen, we always throwing exception in case of failure
      // Leaving as it is just to be sure
      throw new ExplanationException(KubernetesExceptionExplanation.FETCH_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.FETCH_MANIFEST_FAILED));
    }

    startNewCommandUnit(Init, true);
    init(k8sRollingDeployRequest, k8sDelegateTaskParams, getCurrentLogCallback());

    startNewCommandUnit(Prepare, true);
    prepareForRolling(k8sDelegateTaskParams, getCurrentLogCallback(), k8sRollingDeployRequest.isInCanaryWorkflow(),
        k8sRollingDeployRequest.isSkipResourceVersioning());
    List<K8sPod> existingPodList = k8sRollingBaseHandler.getExistingPods(
        steadyStateTimeoutInMillis, managedWorkloads, kubernetesConfig, releaseName, getCurrentLogCallback(), false);

    startNewCommandUnit(Apply, true);
    success = k8sTaskHelperBase.applyManifests(
        client, resources, k8sDelegateTaskParams, getCurrentLogCallback(), true, false);
    if (!success) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.APPLY_MANIFEST_FAILED,
          KubernetesExceptionExplanation.APPLY_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.APPLY_MANIFEST_FAILED));
    }

    shouldSaveReleaseHistory = true;
    startNewCommandUnit(WaitForSteadyState, true);
    if (isEmpty(managedWorkloads)) {
      getCurrentLogCallback().saveExecutionLog(
          "Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      k8sRollingBaseHandler.setManagedWorkloadsInRelease(k8sDelegateTaskParams, managedWorkloads, release, client);

      kubernetesContainerService.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sRollingDeployRequest.getReleaseName(), releaseHistory.getAsYaml());

      List<KubernetesResourceId> managedWorkloadKubernetesResourceIds =
          managedWorkloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());

      success = k8sTaskHelperBase.doStatusCheckForAllResources(client, managedWorkloadKubernetesResourceIds,
          k8sDelegateTaskParams, kubernetesConfig.getNamespace(), getCurrentLogCallback(), true, false);

      // We have to update the DeploymentConfig revision again as the rollout history command sometimes gives the older
      // revision. There seems to be delay in handling of the DeploymentConfig where it still gives older revision even
      // after the apply command has successfully run
      k8sRollingBaseHandler.updateManagedWorkloadsRevision(k8sDelegateTaskParams, release, client);

      if (!success) {
        String managedWorkloadsInfo = managedWorkloadKubernetesResourceIds.stream()
                                          .map(KubernetesResourceId::namespaceKindNameRef)
                                          .collect(Collectors.joining(", "));
        throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED,
            KubernetesExceptionExplanation.WAIT_FOR_STEADY_STATE_FAILED
                + ". Managed workloads: " + managedWorkloadsInfo,
            new KubernetesTaskException(KubernetesExceptionMessage.WAIT_FOR_STEADY_STATE_FAILED));
      }
    }

    startNewCommandUnit(WrapUp, true);
    k8sRollingBaseHandler.wrapUp(k8sDelegateTaskParams, getCurrentLogCallback(), client);

    String loadBalancer = k8sTaskHelperBase.getLoadBalancerEndpoint(kubernetesConfig, resources);
    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder()
            .releaseNumber(release.getNumber())
            .k8sPodList(k8sTaskHelperBase.tagNewPods(k8sRollingBaseHandler.getPods(steadyStateTimeoutInMillis,
                                                         managedWorkloads, kubernetesConfig, releaseName),
                existingPodList))
            .loadBalancer(loadBalancer)
            .build();

    saveRelease(k8sRollingDeployRequest, Status.Succeeded);
    getCurrentLogCallback().saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(rollingSetupResponse)
        .build();
  }

  @Override
  public boolean isErrorFrameworkSupported() {
    return true;
  }

  @Override
  protected void onTaskFailed(K8sDeployRequest deployRequest) {
    if (shouldSaveReleaseHistory) {
      K8sRollingDeployRequest k8sRollingDeployRequest = (K8sRollingDeployRequest) deployRequest;
      saveRelease(k8sRollingDeployRequest, Status.Failed);
    }
  }

  private void saveRelease(K8sRollingDeployRequest k8sRollingDeployRequest, Status status) {
    try {
      releaseHistory.setReleaseStatus(status);
      kubernetesContainerService.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sRollingDeployRequest.getReleaseName(), releaseHistory.getAsYaml());
    } catch (YamlException ex) {
      log.error("Failed to save release history", ex);
      throw new KubernetesTaskException(
          KubernetesExceptionMessage.RELEASE_HISTORY_YAML_EXCEPTION + ". " + ex.getMessage());
    }
  }

  @VisibleForTesting
  void init(K8sRollingDeployRequest request, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    kubernetesConfig =
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(request.getK8sInfraDelegateConfig());
    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData =
        kubernetesContainerService.fetchReleaseHistoryFromConfigMap(kubernetesConfig, request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

    List<String> manifestHelperFiles =
        isEmpty(request.getValuesYamlList()) ? request.getOpenshiftParamList() : request.getValuesYamlList();
    List<FileData> manifestFiles = k8sTaskHelperBase.renderTemplate(k8sDelegateTaskParams,
        request.getManifestDelegateConfig(), manifestFilesDirectory, manifestHelperFiles, releaseName,
        kubernetesConfig.getNamespace(), executionLogCallback, request.getTimeoutIntervalInMin());

    resources = k8sTaskHelperBase.readManifestAndOverrideLocalSecrets(
        manifestFiles, executionLogCallback, request.isLocalOverrideFeatureFlag());
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, kubernetesConfig.getNamespace());

    if (request.isInCanaryWorkflow()) {
      k8sRollingBaseHandler.updateDestinationRuleWithSubsets(executionLogCallback, resources, kubernetesConfig);
      k8sRollingBaseHandler.updateVirtualServiceWithRoutes(executionLogCallback, resources, kubernetesConfig);
    }

    executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

    if (request.isSkipDryRun()) {
      executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return;
    }

    if (!k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback)) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.DRY_RUN_MANIFEST_FAILED,
          KubernetesExceptionExplanation.DRY_RUN_MANIFEST_FAILED,
          new KubernetesTaskException(KubernetesExceptionMessage.DRY_RUN_MANIFEST_FAILED));
    }
  }

  private void prepareForRolling(K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback,
      boolean inCanaryWorkflow, boolean skipResourceVersioning) throws Exception {
    managedWorkloads = getWorkloads(resources);
    if (isNotEmpty(managedWorkloads) && !skipResourceVersioning) {
      markVersionedResources(resources);
    }

    executionLogCallback.saveExecutionLog(
        "Manifests processed. Found following resources: \n" + k8sTaskHelperBase.getResourcesInTableFormat(resources));

    if (!inCanaryWorkflow) {
      release = releaseHistory.createNewRelease(
          resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));
    } else {
      release = releaseHistory.getLatestRelease();
      release.setResources(resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));
    }

    executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

    k8sTaskHelperBase.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

    if (isEmpty(managedWorkloads)) {
      executionLogCallback.saveExecutionLog(color("\nNo Managed Workload found.", Yellow, Bold));
    } else {
      executionLogCallback.saveExecutionLog(color("\nFound following Managed Workloads: \n", Cyan, Bold)
          + k8sTaskHelperBase.getResourcesInTableFormat(managedWorkloads));

      if (!skipResourceVersioning) {
        executionLogCallback.saveExecutionLog("\nVersioning resources.");
        addRevisionNumber(resources, release.getNumber());
      }

      k8sRollingBaseHandler.addLabelsInManagedWorkloadPodSpec(inCanaryWorkflow, managedWorkloads, releaseName);
      k8sRollingBaseHandler.addLabelsInDeploymentSelectorForCanary(inCanaryWorkflow, managedWorkloads);
    }
  }
}
