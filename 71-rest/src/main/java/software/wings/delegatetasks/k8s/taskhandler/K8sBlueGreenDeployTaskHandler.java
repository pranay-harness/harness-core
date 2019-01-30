package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getPrimaryService;
import static io.harness.k8s.manifest.ManifestHelper.getStageService;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static java.util.Arrays.asList;
import static software.wings.beans.Log.LogColor.Blue;
import static software.wings.beans.Log.LogColor.Green;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
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
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class K8sBlueGreenDeployTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sBlueGreenDeployTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

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

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sBlueGreenDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sBlueGreenDeployTaskParameters"));
    }

    K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters =
        (K8sBlueGreenDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sBlueGreenDeployTaskParameters.getReleaseName();

    List<ManifestFile> manifestFiles =
        k8sTaskHelper.fetchManifestFiles(k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig(),
            k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, FetchFiles), gitService,
            encryptionService);
    if (manifestFiles == null) {
      return getFailureResponse();
    }
    k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success = init(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForBlueGreen(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    currentRelease.setManagedWorkload(managedWorkload.getResourceId().cloneInternal());

    success = k8sTaskHelper.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Apply));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse();
    }

    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    currentRelease.setManagedWorkloadRevision(
        k8sTaskHelper.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    success = k8sTaskHelper.doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WaitForSteadyState));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse();
    }

    wrapUp(k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WrapUp));

    List<K8sPod> podList = k8sTaskHelper.getPodDetailsWithColor(
        kubernetesConfig, managedWorkload.getResourceId().getNamespace(), releaseName, stageColor);

    releaseHistory.setReleaseStatus(Status.Succeeded);
    success = kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
    if (!success) {
      logger.error("Failed to save release history");
      return getFailureResponse();
    }

    return k8sTaskHelper.getK8sTaskExecutionResponse(K8sBlueGreenDeployResponse.builder()
                                                         .releaseNumber(currentRelease.getNumber())
                                                         .k8sPodList(podList)
                                                         .primaryServiceName(primaryService.getResourceId().getName())
                                                         .stageServiceName(stageService.getResourceId().getName())
                                                         .build(),
        SUCCESS);
  }

  private boolean init(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sBlueGreenDeployTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), k8sBlueGreenDeployTaskParameters.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig().getManifestFiles(),
          k8sBlueGreenDeployTaskParameters.getValuesYamlList(), executionLogCallback);

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

  private boolean prepareForBlueGreen(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      List<KubernetesResource> workloads = getWorkloads(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do Blue/Green Deployment.", ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nMore than one workloads found in the Manifests. Only one can be managed. Others should be marked with annotation "
                  + HarnessAnnotations.directApply + ": true",
              ERROR, FAILURE);
        }
        return false;
      }

      markVersionedResources(resources);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      primaryService = getPrimaryService(resources);
      stageService = getStageService(resources);

      Service primaryServiceInCluster;
      Service stageServiceInCluster;

      try {
        primaryServiceInCluster = kubernetesContainerService.getService(
            kubernetesConfig, Collections.emptyList(), primaryService.getResourceId().getName());
        if (primaryServiceInCluster == null) {
          executionLogCallback.saveExecutionLog(
              "Primary Service [" + primaryService.getResourceId().getName() + "] not found in cluster.");
        }

        stageServiceInCluster = kubernetesContainerService.getService(
            kubernetesConfig, Collections.emptyList(), stageService.getResourceId().getName());
        if (stageServiceInCluster == null) {
          executionLogCallback.saveExecutionLog(
              "Stage Service [" + stageService.getResourceId().getName() + "] not found in cluster.");
        }

        primaryColor = (primaryServiceInCluster != null) ? getColorFromService(primaryServiceInCluster)
                                                         : HarnessLabelValues.colorDefault;
        stageColor = getInverseColor(primaryColor);

      } catch (Exception e) {
        executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, FAILURE);
        return false;
      }

      currentRelease = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      cleanupForBlueGreen(k8sDelegateTaskParams, releaseHistory, executionLogCallback);

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
          + color(managedWorkload.getResourceId().kindNameRef(), getLogColor(stageColor), Bold));

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public void cleanupForBlueGreen(K8sDelegateTaskParams k8sDelegateTaskParams, ReleaseHistory releaseHistory,
      ExecutionLogCallback executionLogCallback) throws Exception {
    if (StringUtils.equals(primaryColor, stageColor)) {
      return;
    }

    executionLogCallback.saveExecutionLog("Primary Service is at color: " + encodeColor(primaryColor));
    executionLogCallback.saveExecutionLog("Stage Service is at color: " + encodeColor(stageColor));

    executionLogCallback.saveExecutionLog("\nCleaning up non primary releases");

    for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
      Release release = releaseHistory.getReleases().get(releaseIndex);
      if (release.getNumber() != currentRelease.getNumber()
          && release.getManagedWorkload().getName().endsWith(stageColor)) {
        for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
          KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
          if (resourceId.isVersioned()) {
            k8sTaskHelper.delete(client, k8sDelegateTaskParams, asList(resourceId), executionLogCallback);
          }
        }
      }
    }
    releaseHistory.getReleases().removeIf(release
        -> release.getNumber() != currentRelease.getNumber()
            && release.getManagedWorkload().getName().endsWith(stageColor));
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelper.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  private String getColorFromService(Service service) {
    return service.getSpec().getSelector().get(HarnessLabels.color);
  }

  private String getInverseColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return HarnessLabelValues.colorGreen;
      case HarnessLabelValues.colorGreen:
        return HarnessLabelValues.colorBlue;
      default:
        unhandled(color);
    }
    return null;
  }

  private String encodeColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return color(color, Blue, Bold);
      case HarnessLabelValues.colorGreen:
        return color(color, Green, Bold);
      default:
        unhandled(color);
    }
    return null;
  }

  private LogColor getLogColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return LogColor.Blue;
      case HarnessLabelValues.colorGreen:
        return LogColor.Green;
      default:
        unhandled(color);
    }
    return null;
  }

  private K8sTaskExecutionResponse getFailureResponse() {
    K8sBlueGreenDeployResponse k8sBlueGreenDeployResponse = K8sBlueGreenDeployResponse.builder().build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(k8sBlueGreenDeployResponse)
        .build();
  }
}
