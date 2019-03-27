package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
public class K8sRollingDeployTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sRollingDeployTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  KubernetesResource managedWorkload;
  List<KubernetesResource> resources;
  private String releaseName;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sRollingDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sRollingDeployTaskParameters"));
    }

    K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters = (K8sRollingDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sRollingDeployTaskParameters.getReleaseName();

    List<ManifestFile> manifestFiles =
        k8sTaskHelper.fetchManifestFiles(k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig(),
            k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, FetchFiles));
    if (manifestFiles == null) {
      return getFailureResponse();
    }
    k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success = init(k8sRollingDeployTaskParameters, k8sDelegateTaskParams,
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

    if (managedWorkload == null) {
      k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      release.setManagedWorkload(managedWorkload.getResourceId());
      release.setManagedWorkloadRevision(
          k8sTaskHelper.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

      success = k8sTaskHelper.doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
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
    success = kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
    if (!success) {
      logger.error("Failed to save release history");
      return getFailureResponse();
    }

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

  private List<K8sPod> getPods() {
    if (managedWorkload != null) {
      return k8sTaskHelper.getPodDetails(kubernetesConfig,
          isNotBlank(managedWorkload.getResourceId().getNamespace()) ? managedWorkload.getResourceId().getNamespace()
                                                                     : kubernetesConfig.getNamespace(),
          releaseName);
    }
    return new ArrayList<>();
  }

  private List<K8sPod> getNewPods(List<K8sPod> existingPods) {
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

  private boolean init(K8sRollingDeployTaskParameters request, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    try {
      String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getReleaseName());

      releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                                 : ReleaseHistory.createFromData(releaseHistoryData);

      List<ManifestFile> manifestFiles =
          k8sTaskHelper.renderTemplate(k8sDelegateTaskParams, request.getK8sDelegateManifestConfig(),
              request.getValuesYamlList(), releaseName, kubernetesConfig.getNamespace(), executionLogCallback);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

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

      List<KubernetesResource> workloads = getWorkloads(resources);

      if (workloads.size() > 1) {
        executionLogCallback.saveExecutionLog(
            "\nMore than one workloads found in the Manifests. Only one can be managed. Others should be marked with annotation "
                + HarnessAnnotations.directApply + ": true",
            ERROR, FAILURE);
        return false;
      }

      if (!k8sRollingDeployTaskParameters.isInCanaryWorkflow()) {
        release = releaseHistory.createNewRelease(
            resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      } else {
        release = releaseHistory.getLatestRelease();
        release.setResources(resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));
      }

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

      k8sTaskHelper.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      managedWorkload = getManagedWorkload(resources);

      if (managedWorkload == null) {
        executionLogCallback.saveExecutionLog(color("\nNo Managed Workload found.", Yellow, Bold));
      } else {
        executionLogCallback.saveExecutionLog(
            "\nManaged Workload is: " + color(managedWorkload.getResourceId().kindNameRef(), Cyan, Bold));

        executionLogCallback.saveExecutionLog("\nVersioning resources.");

        addRevisionNumber(resources, release.getNumber());

        Map<String, String> podLabels = k8sRollingDeployTaskParameters.isInCanaryWorkflow()
            ? ImmutableMap.of(
                  HarnessLabels.releaseName, releaseName, HarnessLabels.track, HarnessLabelValues.trackStable)
            : ImmutableMap.of(HarnessLabels.releaseName, releaseName);

        managedWorkload.addLabelsInPodSpec(podLabels);

        if (k8sRollingDeployTaskParameters.isInCanaryWorkflow()) {
          managedWorkload.addLabelsInDeploymentSelector(
              ImmutableMap.of(HarnessLabels.track, HarnessLabelValues.trackStable));
        }
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
}
