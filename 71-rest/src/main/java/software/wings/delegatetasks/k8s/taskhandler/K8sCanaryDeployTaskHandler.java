package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogColor;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCanaryDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sCanaryDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class K8sCanaryDeployTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sCanaryDeployTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release currentRelease;
  private KubernetesResource canaryWorkload;
  private List<KubernetesResource> resources;
  private Integer targetInstances;
  private KubernetesResourceId previousManagedWorkload;
  private String releaseName;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sCanaryDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sCanaryDeployTaskParameters"));
    }

    K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters = (K8sCanaryDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sCanaryDeployTaskParameters.getReleaseName();

    List<ManifestFile> manifestFiles =
        k8sTaskHelper.fetchManifestFiles(k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig(),
            getLogCallBack(k8sCanaryDeployTaskParameters, FetchFiles), gitService, encryptionService);
    if (manifestFiles == null) {
      return getFailureResponse();
    }
    k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success =
        init(k8sCanaryDeployTaskParameters, k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForCanary(
        k8sDelegateTaskParams, k8sCanaryDeployTaskParameters, getLogCallBack(k8sCanaryDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    success = k8sTaskHelper.applyManifests(
        client, resources, k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, Apply));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sCanaryDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse();
    }

    success = k8sTaskHelper.doStatusCheck(client, canaryWorkload.getResourceId(), k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sCanaryDeployTaskParameters, WaitForSteadyState));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sCanaryDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse();
    }

    List<K8sPod> podList = k8sTaskHelper.getPodDetailsWithTrack(
        kubernetesConfig, canaryWorkload.getResourceId().getNamespace(), releaseName, "canary");

    wrapUp(k8sDelegateTaskParams, getLogCallBack(k8sCanaryDeployTaskParameters, WrapUp));

    success = kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sCanaryDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
    if (!success) {
      logger.error("Failed to save release history");
      return getFailureResponse();
    }

    K8sCanaryDeployResponse k8sCanaryDeployResponse =
        K8sCanaryDeployResponse.builder()
            .releaseNumber(currentRelease.getNumber())
            .k8sPodList(podList)
            .currentInstances(targetInstances)
            .canaryWorkload(canaryWorkload.getResourceId().namespaceKindNameRef())
            .build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sCanaryDeployResponse)
        .build();
  }

  private K8sTaskExecutionResponse getFailureResponse() {
    K8sCanaryDeployResponse k8sCanaryDeployResponse = K8sCanaryDeployResponse.builder().build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(k8sCanaryDeployResponse)
        .build();
  }

  private ExecutionLogCallback getLogCallBack(K8sCanaryDeployTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  private boolean init(K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sCanaryDeployTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), k8sCanaryDeployTaskParameters.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          k8sCanaryDeployTaskParameters.getK8sDelegateManifestConfig().getManifestFiles(),
          k8sCanaryDeployTaskParameters.getValuesYamlList(), executionLogCallback);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

    return k8sTaskHelper.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
  }

  private boolean prepareForCanary(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sCanaryDeployTaskParameters k8sCanaryDeployTaskParameters, ExecutionLogCallback executionLogCallback) {
    try {
      markVersionedResources(resources, false);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      List<KubernetesResource> workloads = getWorkloads(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do Canary Deployment.", ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nMore than one workloads found in the Manifests. Only one can be managed. Others should be marked with annotation "
                  + HarnessAnnotations.directApply + ": true",
              ERROR, FAILURE);
        }
        return false;
      }

      currentRelease = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, currentRelease.getNumber(), false);
      canaryWorkload = getManagedWorkload(resources);

      k8sTaskHelper.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      Integer currentInstances =
          k8sTaskHelper.getCurrentReplicas(client, canaryWorkload.getResourceId(), k8sDelegateTaskParams);
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
          targetInstances = (int) Math.round(k8sCanaryDeployTaskParameters.getInstances() * maxInstances / 100.0);
          break;

        default:
          unhandled(k8sCanaryDeployTaskParameters.getInstanceUnitType());
      }

      canaryWorkload.appendSuffixInName("-canary");
      canaryWorkload.addReleaseLabelsInPodSpec(releaseName, currentRelease.getNumber(), HarnessLabelValues.trackCanary);
      canaryWorkload.addTrackLabelInDeploymentSelector(HarnessLabelValues.trackCanary);
      canaryWorkload.setReplicaCount(targetInstances);

      executionLogCallback.saveExecutionLog(
          "\nCanary Workload is: " + color(canaryWorkload.getResourceId().kindNameRef(), LogColor.Cyan, Bold));

      executionLogCallback.saveExecutionLog("\nTarget replica count for Canary is " + targetInstances);

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
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
