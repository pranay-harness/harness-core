package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static java.util.Arrays.asList;
import static software.wings.beans.Log.LogColor.Cyan;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;
import static software.wings.delegatetasks.k8s.K8sTask.MANIFEST_FILES_DIR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
public class K8sRollingDeployTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  List<KubernetesResource> managedWorkloads;
  List<KubernetesResource> resources;
  private String releaseName;
  private String manifestFilesDirectory;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sRollingDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sRollingDeployTaskParameters"));
    }

    K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters = (K8sRollingDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sRollingDeployTaskParameters.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();

    boolean success = k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(
        k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, FetchFiles));
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sRollingDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForRolling(k8sRollingDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    List<K8sPod> existingPodList = getPods();

    success = k8sTaskHelper.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Apply));
    if (!success) {
      return getFailureResponse();
    }

    if (isEmpty(managedWorkloads)) {
      k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      setManagedWorkloadsInRelease(k8sDelegateTaskParams);

      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

      List<KubernetesResourceId> managedWorkloadKubernetesResourceIds =
          managedWorkloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());
      success = k8sTaskHelper.doStatusCheckForAllResources(client, managedWorkloadKubernetesResourceIds,
          k8sDelegateTaskParams, kubernetesConfig.getNamespace(),
          k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState));
      if (!success) {
        releaseHistory.setReleaseStatus(Status.Failed);
        kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
            k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
        return getFailureResponse();
      }
    }

    wrapUp(k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WrapUp));

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder()
            .releaseNumber(release.getNumber())
            .k8sPodList(getNewPods(existingPodList))
            .loadBalancer(k8sTaskHelper.getLoadBalancerEndpoint(kubernetesConfig, resources))
            .build();

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(rollingSetupResponse)
        .build();
  }

  private List<K8sPod> getPods() throws Exception {
    List<K8sPod> k8sPods = new ArrayList<>();

    if (isEmpty(managedWorkloads)) {
      return k8sPods;
    }

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      List<K8sPod> podDetails =
          k8sTaskHelper.getPodDetails(kubernetesConfig, kubernetesResource.getResourceId().getNamespace(), releaseName);

      if (isNotEmpty(podDetails)) {
        k8sPods.addAll(podDetails);
      }
    }

    return k8sPods;
  }

  private List<K8sPod> getNewPods(List<K8sPod> existingPods) throws Exception {
    List<K8sPod> newPods = getPods();
    if (CollectionUtils.isEmpty(existingPods)) {
      return newPods;
    } else {
      Set<String> existingPodNames = existingPods.stream().map(K8sPod::getName).collect(Collectors.toSet());
      return newPods.stream().filter(pod -> !existingPodNames.contains(pod.getName())).collect(Collectors.toList());
    }
  }

  private K8sTaskExecutionResponse getFailureResponse() {
    K8sRollingDeployResponse rollingSetupResponse = K8sRollingDeployResponse.builder().build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(rollingSetupResponse)
        .build();
  }

  @VisibleForTesting
  boolean init(K8sRollingDeployTaskParameters request, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());
    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    try {
      String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getReleaseName());

      releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                                 : ReleaseHistory.createFromData(releaseHistoryData);

      k8sTaskHelper.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          request.getK8sDelegateManifestConfig(), manifestFilesDirectory, request.getValuesYamlList(), releaseName,
          kubernetesConfig.getNamespace(), executionLogCallback, request);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);
      k8sTaskHelper.setNamespaceToKubernetesResourcesIfRequired(resources, kubernetesConfig.getNamespace());

      if (request.isInCanaryWorkflow()) {
        updateDestinationRuleManifestFilesWithSubsets(executionLogCallback);
        updateVirtualServiceManifestFilesWithRoutes(executionLogCallback);
      }

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

      if (request.isSkipDryRun()) {
        executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      }

      return k8sTaskHelper.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }
  }

  private boolean prepareForRolling(K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      markVersionedResources(resources);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      if (!k8sRollingDeployTaskParameters.isInCanaryWorkflow()) {
        release = releaseHistory.createNewRelease(
            resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));
      } else {
        release = releaseHistory.getLatestRelease();
        release.setResources(resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));
      }

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

      k8sTaskHelper.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      managedWorkloads = getWorkloads(resources);

      if (isEmpty(managedWorkloads)) {
        executionLogCallback.saveExecutionLog(color("\nNo Managed Workload found.", Yellow, Bold));
      } else {
        executionLogCallback.saveExecutionLog(color("\nFound following Managed Workloads: \n", Cyan, Bold)
            + k8sTaskHelper.getResourcesInTableFormat(managedWorkloads));

        executionLogCallback.saveExecutionLog("\nVersioning resources.");
        addRevisionNumber(resources, release.getNumber());

        addLabelsInManagedWorkloadPodSpec(k8sRollingDeployTaskParameters);
        addLabelsInDeploymentSelectorForCanary(k8sRollingDeployTaskParameters);
      }
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelper.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  private void updateVirtualServiceManifestFilesWithRoutes(ExecutionLogCallback executionLogCallback)
      throws IOException {
    k8sTaskHelper.updateVirtualServiceManifestFilesWithRoutesForCanary(
        resources, kubernetesConfig, executionLogCallback);
  }

  private void updateDestinationRuleManifestFilesWithSubsets(ExecutionLogCallback executionLogCallback)
      throws IOException {
    k8sTaskHelper.updateDestinationRuleManifestFilesWithSubsets(resources,
        asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), kubernetesConfig, executionLogCallback);
  }

  private void addLabelsInManagedWorkloadPodSpec(K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters) {
    Map<String, String> podLabels = k8sRollingDeployTaskParameters.isInCanaryWorkflow()
        ? ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, HarnessLabelValues.trackStable)
        : ImmutableMap.of(HarnessLabels.releaseName, releaseName);

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      kubernetesResource.addLabelsInPodSpec(podLabels);
    }
  }

  private void addLabelsInDeploymentSelectorForCanary(K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters) {
    if (!k8sRollingDeployTaskParameters.isInCanaryWorkflow()) {
      return;
    }

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      if (Kind.Deployment.name().equals(kubernetesResource.getResourceId().getKind())) {
        kubernetesResource.addLabelsInDeploymentSelector(
            ImmutableMap.of(HarnessLabels.track, HarnessLabelValues.trackStable));
      }
    }
  }

  private void setManagedWorkloadsInRelease(K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    List<KubernetesResourceIdRevision> kubernetesResourceIdRevisions = new ArrayList<>();

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      String latestRevision =
          k8sTaskHelper.getLatestRevision(client, kubernetesResource.getResourceId(), k8sDelegateTaskParams);

      kubernetesResourceIdRevisions.add(KubernetesResourceIdRevision.builder()
                                            .workload(kubernetesResource.getResourceId())
                                            .revision(latestRevision)
                                            .build());
    }

    release.setManagedWorkloads(kubernetesResourceIdRevisions);
  }
}
