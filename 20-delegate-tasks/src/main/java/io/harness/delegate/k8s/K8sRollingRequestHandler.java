package io.harness.delegate.k8s;

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
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.beans.FileData;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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

  @Override
  protected K8sDeployResponse executeTaskInternal(
      K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sDeployRequest instanceof K8sRollingDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sRollingDeployRequest"));
    }

    K8sRollingDeployRequest k8sRollingDeployRequest = (K8sRollingDeployRequest) k8sDeployRequest;

    releaseName = k8sRollingDeployRequest.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sDeployRequest.getTimeoutIntervalInMin());

    boolean success =
        k8sTaskHelperBase.fetchManifestFilesAndWriteToDirectory(k8sRollingDeployRequest.getManifestDelegateConfig(),
            manifestFilesDirectory, k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, FetchFiles),
            steadyStateTimeoutInMillis, k8sRollingDeployRequest.getAccountId());
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sRollingDeployRequest, k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForRolling(k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, Prepare),
        k8sRollingDeployRequest.isInCanaryWorkflow());
    if (!success) {
      return getFailureResponse();
    }

    List<K8sPod> existingPodList =
        k8sRollingBaseHandler.getPods(steadyStateTimeoutInMillis, managedWorkloads, kubernetesConfig, releaseName);

    success = k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, Apply), true);
    if (!success) {
      return getFailureResponse();
    }

    if (isEmpty(managedWorkloads)) {
      k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      k8sRollingBaseHandler.setManagedWorkloadsInRelease(k8sDelegateTaskParams, managedWorkloads, release, client);

      kubernetesContainerService.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sRollingDeployRequest.getReleaseName(), releaseHistory.getAsYaml());

      List<KubernetesResourceId> managedWorkloadKubernetesResourceIds =
          managedWorkloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());
      success = k8sTaskHelperBase.doStatusCheckForAllResources(client, managedWorkloadKubernetesResourceIds,
          k8sDelegateTaskParams, kubernetesConfig.getNamespace(),
          k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, WaitForSteadyState), true);

      // We have to update the DeploymentConfig revision again as the rollout history command sometimes gives the older
      // revision. There seems to be delay in handling of the DeploymentConfig where it still gives older revision even
      // after the apply command has successfully run
      k8sRollingBaseHandler.updateDeploymentConfigRevision(k8sDelegateTaskParams, release, client);

      if (!success) {
        releaseHistory.setReleaseStatus(Status.Failed);
        kubernetesContainerService.saveReleaseHistoryInConfigMap(
            kubernetesConfig, k8sRollingDeployRequest.getReleaseName(), releaseHistory.getAsYaml());
        return getFailureResponse();
      }
    }

    k8sRollingBaseHandler.wrapUp(
        k8sDelegateTaskParams, k8sTaskHelperBase.getExecutionLogCallback(k8sRollingDeployRequest, WrapUp), client);

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistoryInConfigMap(
        kubernetesConfig, k8sRollingDeployRequest.getReleaseName(), releaseHistory.getAsYaml());

    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder()
            .releaseNumber(release.getNumber())
            .k8sPodList(k8sRollingBaseHandler.tagNewPods(k8sRollingBaseHandler.getPods(steadyStateTimeoutInMillis,
                                                             managedWorkloads, kubernetesConfig, releaseName),
                existingPodList))
            .loadBalancer(k8sTaskHelperBase.getLoadBalancerEndpoint(kubernetesConfig, resources))
            .build();

    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(rollingSetupResponse)
        .build();
  }

  private K8sDeployResponse getFailureResponse() {
    K8sRollingDeployResponse rollingSetupResponse = K8sRollingDeployResponse.builder().build();
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sNGTaskResponse(rollingSetupResponse)
        .build();
  }

  @VisibleForTesting
  boolean init(
      K8sRollingDeployRequest request, K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    kubernetesConfig =
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(request.getK8sInfraDelegateConfig());
    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    try {
      String releaseHistoryData =
          kubernetesContainerService.fetchReleaseHistoryFromConfigMap(kubernetesConfig, request.getReleaseName());

      releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                                 : ReleaseHistory.createFromData(releaseHistoryData);

      k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<FileData> manifestFiles = k8sTaskHelperBase.renderTemplate(k8sDelegateTaskParams,
          request.getManifestDelegateConfig(), manifestFilesDirectory, request.getValuesYamlList(), releaseName,
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
        return true;
      }

      return k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }
  }

  private boolean prepareForRolling(
      K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback, boolean inCanaryWorkflow) {
    try {
      managedWorkloads = getWorkloads(resources);
      if (isNotEmpty(managedWorkloads)) {
        markVersionedResources(resources);
      }

      executionLogCallback.saveExecutionLog("Manifests processed. Found following resources: \n"
          + k8sTaskHelperBase.getResourcesInTableFormat(resources));

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

        executionLogCallback.saveExecutionLog("\nVersioning resources.");
        addRevisionNumber(resources, release.getNumber());

        k8sRollingBaseHandler.addLabelsInManagedWorkloadPodSpec(inCanaryWorkflow, managedWorkloads, releaseName);
        k8sRollingBaseHandler.addLabelsInDeploymentSelectorForCanary(inCanaryWorkflow, managedWorkloads);
      }
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }
}
