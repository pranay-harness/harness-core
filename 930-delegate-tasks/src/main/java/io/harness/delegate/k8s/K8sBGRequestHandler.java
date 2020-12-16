package io.harness.delegate.k8s;

import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.k8s.manifest.ManifestHelper.getKubernetesResourceFromSpec;
import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getPrimaryService;
import static io.harness.k8s.manifest.ManifestHelper.getServices;
import static io.harness.k8s.manifest.ManifestHelper.getStageService;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloadsForCanaryAndBG;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.beans.FileData;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.Service;
import io.kubernetes.client.openapi.models.V1Service;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
public class K8sBGRequestHandler extends K8sRequestHandler {
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private K8sBGBaseHandler k8sBGBaseHandler;
  @Inject private KubernetesContainerService kubernetesContainerService;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release currentRelease;
  private KubernetesResource managedWorkload;
  private List<KubernetesResource> resources;
  private KubernetesResource primaryService;
  private KubernetesResource stageService;
  private String primaryColor;
  private String stageColor;
  private String releaseName;
  private String manifestFilesDirectory;
  private boolean isDeprecateFabric8Enabled;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient) throws Exception {
    if (!(k8sDeployRequest instanceof K8sBGDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sBGDeployRequest"));
    }

    K8sBGDeployRequest k8sBGDeployRequest = (K8sBGDeployRequest) k8sDeployRequest;

    isDeprecateFabric8Enabled = k8sBGDeployRequest.isDeprecateFabric8Enabled();
    releaseName = k8sBGDeployRequest.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    final long timeoutInMillis = getTimeoutMillisFromMinutes(k8sBGDeployRequest.getTimeoutIntervalInMin());

    boolean success =
        k8sTaskHelperBase.fetchManifestFilesAndWriteToDirectory(k8sBGDeployRequest.getManifestDelegateConfig(),
            manifestFilesDirectory, k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, FetchFiles),
            timeoutInMillis, k8sBGDeployRequest.getAccountId());
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sBGDeployRequest, k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForBlueGreen(k8sBGDeployRequest, k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    currentRelease.setManagedWorkload(managedWorkload.getResourceId().cloneInternal());

    success = k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, Apply), true);
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sBGDeployRequest.getReleaseName(),
          releaseHistory.getAsYaml(), k8sBGDeployRequest.isDeprecateFabric8Enabled());
      return getFailureResponse();
    }

    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sBGDeployRequest.getReleaseName(),
        releaseHistory.getAsYaml(), k8sBGDeployRequest.isDeprecateFabric8Enabled());

    currentRelease.setManagedWorkloadRevision(
        k8sTaskHelperBase.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    success = k8sTaskHelperBase.doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
        k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, WaitForSteadyState));

    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sBGDeployRequest.getReleaseName(),
          releaseHistory.getAsYaml(), k8sBGDeployRequest.isDeprecateFabric8Enabled());
      return getFailureResponse();
    }

    k8sBGBaseHandler.wrapUp(
        k8sDelegateTaskParams, k8sTaskHelperBase.getExecutionLogCallback(logStreamingTaskClient, WrapUp), client);

    final List<K8sPod> podList = k8sBGBaseHandler.getAllPods(timeoutInMillis, kubernetesConfig, managedWorkload,
        primaryColor, stageColor, releaseName, isDeprecateFabric8Enabled);

    currentRelease.setManagedWorkloadRevision(
        k8sTaskHelperBase.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));
    releaseHistory.setReleaseStatus(Status.Succeeded);
    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sBGDeployRequest.getReleaseName(),
        releaseHistory.getAsYaml(), k8sBGDeployRequest.isDeprecateFabric8Enabled());

    K8sBGDeployResponse k8sBGDeployResponse = K8sBGDeployResponse.builder()
                                                  .releaseNumber(currentRelease.getNumber())
                                                  .k8sPodList(podList)
                                                  .primaryServiceName(primaryService.getResourceId().getName())
                                                  .stageServiceName(stageService.getResourceId().getName())
                                                  .stageColor(stageColor)
                                                  .primaryColor(primaryColor)
                                                  .build();
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(k8sBGDeployResponse)
        .build();
  }

  private K8sDeployResponse getFailureResponse() {
    K8sBGDeployResponse k8sBGDeployResponse = K8sBGDeployResponse.builder().build();
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sNGTaskResponse(k8sBGDeployResponse)
        .build();
  }

  @VisibleForTesting
  boolean init(K8sBGDeployRequest request, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(request.getK8sInfraDelegateConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());
    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(
        kubernetesConfig, request.getReleaseName(), request.isDeprecateFabric8Enabled());
    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<FileData> manifestFiles = k8sTaskHelperBase.renderTemplate(k8sDelegateTaskParams,
          request.getManifestDelegateConfig(), manifestFilesDirectory, request.getValuesYamlList(), releaseName,
          kubernetesConfig.getNamespace(), executionLogCallback, request.getTimeoutIntervalInMin());

      resources = k8sTaskHelperBase.readManifests(manifestFiles, executionLogCallback);
      k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, kubernetesConfig.getNamespace());
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

    if (request.isSkipDryRun()) {
      executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    return k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
  }

  @VisibleForTesting
  boolean prepareForBlueGreen(
      K8sBGDeployRequest request, K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback) {
    try {
      markVersionedResources(resources);

      executionLogCallback.saveExecutionLog("Manifests processed. Found following resources: \n"
          + k8sTaskHelperBase.getResourcesInTableFormat(resources));

      List<KubernetesResource> workloads = getWorkloadsForCanaryAndBG(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do  Blue/Green Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Blue/Green workflow type.",
              ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nThere are multiple workloads in the Service Manifests you are deploying. Blue/Green Workflows support a single Deployment or DeploymentConfig (OpenShift) workload only. To deploy additional workloads in Manifests, annotate them with "
                  + HarnessAnnotations.directApply + ": true",
              ERROR, FAILURE);
        }
        return false;
      }

      primaryService = getPrimaryService(resources);
      stageService = getStageService(resources);

      if (primaryService == null) {
        List<KubernetesResource> services = getServices(resources);
        if (services.size() == 1) {
          primaryService = services.get(0);
          executionLogCallback.saveExecutionLog(
              "Primary Service is " + color(primaryService.getResourceId().getName(), White, Bold));
        } else if (services.size() == 0) {
          throw new KubernetesYamlException(
              "No service is found in manifests. Service is required for BlueGreen deployments."
              + " Add at least one service manifest. Two services [i.e. primary and stage] can be specified with annotations "
              + HarnessAnnotations.primaryService + " and " + HarnessAnnotations.stageService);
        } else {
          throw new KubernetesYamlException(
              "Could not locate a Primary Service in Manifests. Primary and Stage services should be annotated with "
              + HarnessAnnotations.primaryService + " and " + HarnessAnnotations.stageService);
        }
      }

      if (stageService == null) {
        // create a clone
        stageService = getKubernetesResourceFromSpec(primaryService.getSpec());
        stageService.appendSuffixInName("-stage");
        resources.add(stageService);
        executionLogCallback.saveExecutionLog(format("Created Stage service [%s] using Spec from Primary Service [%s]",
            stageService.getResourceId().getName(), primaryService.getResourceId().getName()));
      }

      try {
        if (request.isDeprecateFabric8Enabled()) {
          primaryColor = k8sBGBaseHandler.getPrimaryColor(primaryService, kubernetesConfig, executionLogCallback);
          V1Service stageServiceInCluster =
              kubernetesContainerService.getService(kubernetesConfig, stageService.getResourceId().getName());
          if (stageServiceInCluster == null) {
            executionLogCallback.saveExecutionLog(
                "Stage Service [" + stageService.getResourceId().getName() + "] not found in cluster.");
          }
        } else {
          primaryColor =
              k8sBGBaseHandler.getPrimaryColorUsingFabric8(primaryService, kubernetesConfig, executionLogCallback);
          Service stageServiceInCluster =
              kubernetesContainerService.getServiceFabric8(kubernetesConfig, stageService.getResourceId().getName());
          if (stageServiceInCluster == null) {
            executionLogCallback.saveExecutionLog(
                "Stage Service [" + stageService.getResourceId().getName() + "] not found in cluster.");
          }
        }

        if (primaryColor == null) {
          executionLogCallback.saveExecutionLog(
              format(
                  "Found conflicting service [%s] in the cluster. For blue/green deployment, the label [harness.io/color] is required in service selector. Delete this existing service to proceed",
                  primaryService.getResourceId().getName()),
              ERROR, FAILURE);
          return false;
        }

        stageColor = k8sBGBaseHandler.getInverseColor(primaryColor);

      } catch (Exception e) {
        log.error("Exception:", e);
        executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, FAILURE);
        return false;
      }

      currentRelease = releaseHistory.createNewRelease(
          resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));

      k8sBGBaseHandler.cleanupForBlueGreen(k8sDelegateTaskParams, releaseHistory, executionLogCallback, primaryColor,
          stageColor, currentRelease, client);

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, currentRelease.getNumber());
      managedWorkload = getManagedWorkload(resources);
      managedWorkload.appendSuffixInName('-' + stageColor);
      managedWorkload.addLabelsInPodSpec(
          ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.color, stageColor));
      managedWorkload.addLabelsInDeploymentSelector(ImmutableMap.of(HarnessLabels.color, stageColor));

      primaryService.addColorSelectorInService(primaryColor);
      stageService.addColorSelectorInService(stageColor);

      executionLogCallback.saveExecutionLog("\nWorkload to deploy is: "
          + color(managedWorkload.getResourceId().kindNameRef(), k8sBGBaseHandler.getLogColor(stageColor), Bold));

    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }
}
