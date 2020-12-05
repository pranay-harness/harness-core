package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
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

import static java.util.Arrays.asList;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileData;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.LogColor;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sCanaryDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sCanaryDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class K8sCanaryDeployTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release currentRelease;
  private KubernetesResource canaryWorkload;
  private List<KubernetesResource> resources;
  private Integer targetInstances;
  private KubernetesResourceId previousManagedWorkload;
  private String releaseName;
  private String manifestFilesDirectory;
  private boolean isDeprecateFabric8Enabled;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sCanaryDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sCanaryDeployTaskParameters"));
    }

    K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters = (K8sCanaryDeployTaskParameters) k8sTaskParameters;

    isDeprecateFabric8Enabled = k8sCanaryDeployTaskParameters.isDeprecateFabric8Enabled();
    releaseName = k8sCanaryDeployTaskParameters.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    final long timeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    boolean success = k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(
        k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
        getLogCallBack(k8sCanaryDeployTaskParameters, FetchFiles), timeoutInMillis);
    if (!success) {
      return getFailureResponse();
    }

    success =
        init(k8sCanaryDeployTaskParameters, k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForCanary(
        k8sDelegateTaskParams, k8sCanaryDeployTaskParameters, getLogCallBack(k8sCanaryDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    success = k8sTaskHelperBase.applyManifests(
        client, resources, k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, Apply), true);
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sCanaryDeployTaskParameters.getReleaseName(),
          releaseHistory.getAsYaml(), k8sCanaryDeployTaskParameters.isDeprecateFabric8Enabled());
      return getFailureResponse();
    }

    ExecutionLogCallback executionLogCallback =
        k8sTaskHelper.getExecutionLogCallback(k8sCanaryDeployTaskParameters, WaitForSteadyState);
    success = k8sTaskHelperBase.doStatusCheck(
        client, canaryWorkload.getResourceId(), k8sDelegateTaskParams, executionLogCallback);

    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sCanaryDeployTaskParameters.getReleaseName(),
          releaseHistory.getAsYaml(), k8sCanaryDeployTaskParameters.isDeprecateFabric8Enabled());
      return getFailureResponse();
    }

    List<K8sPod> allPods = getAllPods(timeoutInMillis);
    HelmChartInfo helmChartInfo = k8sTaskHelper.getHelmChartDetails(
        k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory);

    wrapUp(k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, WrapUp));

    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, k8sCanaryDeployTaskParameters.getReleaseName(),
        releaseHistory.getAsYaml(), k8sCanaryDeployTaskParameters.isDeprecateFabric8Enabled());

    K8sCanaryDeployResponse k8sCanaryDeployResponse =
        K8sCanaryDeployResponse.builder()
            .releaseNumber(currentRelease.getNumber())
            .k8sPodList(allPods)
            .currentInstances(targetInstances)
            .canaryWorkload(canaryWorkload.getResourceId().namespaceKindNameRef())
            .helmChartInfo(helmChartInfo)
            .build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sCanaryDeployResponse)
        .build();
  }

  @VisibleForTesting
  List<K8sPod> getAllPods(long timeoutInMillis) throws Exception {
    String namespace = canaryWorkload.getResourceId().getNamespace();
    List<K8sPod> allPods = isDeprecateFabric8Enabled
        ? k8sTaskHelperBase.getPodDetails(kubernetesConfig, namespace, releaseName, timeoutInMillis)
        : k8sTaskHelperBase.getPodDetailsFabric8(kubernetesConfig, namespace, releaseName, timeoutInMillis);
    List<K8sPod> canaryPods = isDeprecateFabric8Enabled
        ? k8sTaskHelperBase.getPodDetailsWithTrack(kubernetesConfig, namespace, releaseName, "canary", timeoutInMillis)
        : k8sTaskHelperBase.getPodDetailsWithTrackFabric8(
            kubernetesConfig, namespace, releaseName, "canary", timeoutInMillis);
    Set<String> canaryPodNames = canaryPods.stream().map(K8sPod::getName).collect(Collectors.toSet());
    allPods.forEach(pod -> {
      if (canaryPodNames.contains(pod.getName())) {
        pod.setNewPod(true);
      }
    });
    return allPods;
  }

  private K8sTaskExecutionResponse getFailureResponse() {
    K8sCanaryDeployResponse k8sCanaryDeployResponse = K8sCanaryDeployResponse.builder().build();
    if (canaryWorkload != null && canaryWorkload.getResourceId() != null) {
      k8sCanaryDeployResponse.setCanaryWorkload(canaryWorkload.getResourceId().namespaceKindNameRef());
    }

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(k8sCanaryDeployResponse)
        .build();
  }

  private ExecutionLogCallback getLogCallBack(K8sCanaryDeployTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  @VisibleForTesting
  boolean init(K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
        k8sCanaryDeployTaskParameters.getK8sClusterConfig(), false);

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(kubernetesConfig,
        k8sCanaryDeployTaskParameters.getReleaseName(), k8sCanaryDeployTaskParameters.isDeprecateFabric8Enabled());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<FileData> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
          k8sCanaryDeployTaskParameters.getValuesYamlList(), releaseName, kubernetesConfig.getNamespace(),
          executionLogCallback, k8sCanaryDeployTaskParameters);

      resources = k8sTaskHelperBase.readManifests(manifestFiles, executionLogCallback);
      k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(resources, kubernetesConfig.getNamespace());

      updateDestinationRuleManifestFilesWithSubsets(executionLogCallback);
      updateVirtualServiceManifestFilesWithRoutes(executionLogCallback);

    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

    if (k8sCanaryDeployTaskParameters.isSkipDryRun()) {
      executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    return k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
  }

  @VisibleForTesting
  boolean prepareForCanary(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters, ExecutionLogCallback executionLogCallback) {
    try {
      markVersionedResources(resources);

      executionLogCallback.saveExecutionLog("Manifests processed. Found following resources: \n"
          + k8sTaskHelperBase.getResourcesInTableFormat(resources));

      List<KubernetesResource> workloads = getWorkloadsForCanaryAndBG(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Canary workflow type.",
              ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nMore than one workloads found in the Manifests. Canary deploy supports only one workload. Others should be marked with annotation "
                  + HarnessAnnotations.directApply + ": true",
              ERROR, FAILURE);
        }
        return false;
      }

      currentRelease = releaseHistory.createNewRelease(
          resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, currentRelease.getNumber());
      canaryWorkload = workloads.get(0);

      k8sTaskHelperBase.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      Integer currentInstances =
          k8sTaskHelperBase.getCurrentReplicas(client, canaryWorkload.getResourceId(), k8sDelegateTaskParams);
      if (currentInstances != null) {
        executionLogCallback.saveExecutionLog("\nCurrent replica count is " + currentInstances);
      }

      if (currentInstances == null) {
        currentInstances = canaryWorkload.getReplicaCount();
      }

      if (currentInstances == null) {
        currentInstances = 1;
      }

      targetInstances = currentInstances;

      switch (k8sCanaryDeployTaskParameters.getInstanceUnitType()) {
        case COUNT:
          targetInstances = k8sCanaryDeployTaskParameters.getInstances();
          break;

        case PERCENTAGE:
          Integer maxInstances;
          if (k8sCanaryDeployTaskParameters.getMaxInstances().isPresent()) {
            maxInstances = k8sCanaryDeployTaskParameters.getMaxInstances().get();
          } else {
            maxInstances = currentInstances;
          }
          targetInstances = k8sTaskHelperBase.getTargetInstancesForCanary(
              k8sCanaryDeployTaskParameters.getInstances(), maxInstances, executionLogCallback);
          break;

        default:
          unhandled(k8sCanaryDeployTaskParameters.getInstanceUnitType());
      }

      canaryWorkload.appendSuffixInName("-canary");
      canaryWorkload.addLabelsInPodSpec(
          ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, HarnessLabelValues.trackCanary));
      canaryWorkload.addLabelsInDeploymentSelector(
          ImmutableMap.of(HarnessLabels.track, HarnessLabelValues.trackCanary));
      canaryWorkload.setReplicaCount(targetInstances);

      executionLogCallback.saveExecutionLog(
          "\nCanary Workload is: " + color(canaryWorkload.getResourceId().kindNameRef(), LogColor.Cyan, Bold));

      executionLogCallback.saveExecutionLog("\nTarget replica count for Canary is " + targetInstances);

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      log.error("Exception:", e);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelperBase.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  private void updateDestinationRuleManifestFilesWithSubsets(ExecutionLogCallback executionLogCallback)
      throws IOException {
    k8sTaskHelperBase.updateDestinationRuleManifestFilesWithSubsets(resources,
        asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), kubernetesConfig, executionLogCallback);
  }

  private void updateVirtualServiceManifestFilesWithRoutes(ExecutionLogCallback executionLogCallback)
      throws IOException {
    k8sTaskHelperBase.updateVirtualServiceManifestFilesWithRoutesForCanary(
        resources, kubernetesConfig, executionLogCallback);
  }
}
