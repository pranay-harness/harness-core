package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.Prune;
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
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileData;
import io.harness.delegate.k8s.K8sBGBaseHandler;
import io.harness.delegate.k8s.PrePruningInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
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

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
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
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class K8sBlueGreenDeployTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private K8sBGBaseHandler k8sBGBaseHandler;

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
  private PrePruningInfo prePruningInfo;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sBlueGreenDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sBlueGreenDeployTaskParameters"));
    }

    K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters =
        (K8sBlueGreenDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sBlueGreenDeployTaskParameters.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    final long timeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    boolean success = k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(
        k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, FetchFiles), timeoutInMillis);
    if (!success) {
      return getFailureResponse(null);
    }

    success = init(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse(null);
    }

    success = prepareForBlueGreen(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse(null);
    }

    currentRelease.setManagedWorkload(managedWorkload.getResourceId().cloneInternal());

    success = k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Apply), true);
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse(null);
    }

    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(
        kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    currentRelease.setManagedWorkloadRevision(
        k8sTaskHelperBase.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    ExecutionLogCallback executionLogCallback =
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WaitForSteadyState);

    success = k8sTaskHelperBase.doStatusCheck(
        client, managedWorkload.getResourceId(), k8sDelegateTaskParams, executionLogCallback);

    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse(null);
    }

    ExecutionLogCallback wrapUpLogCallback =
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WrapUp);
    try {
      HelmChartInfo helmChartInfo = k8sTaskHelper.getHelmChartDetails(
          k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory);
      k8sBGBaseHandler.wrapUp(k8sDelegateTaskParams, wrapUpLogCallback, client);
      final List<K8sPod> podList = k8sBGBaseHandler.getAllPods(
          timeoutInMillis, kubernetesConfig, managedWorkload, primaryColor, stageColor, releaseName);

      currentRelease.setManagedWorkloadRevision(
          k8sTaskHelperBase.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));
      releaseHistory.setReleaseStatus(Status.Succeeded);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

      wrapUpLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

      if (k8sBlueGreenDeployTaskParameters.isPruningEnabled()) {
        ExecutionLogCallback pruneExecutionLogCallback =
            k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Prune);
        k8sBGBaseHandler.pruneForBg(k8sDelegateTaskParams, prePruningInfo, pruneExecutionLogCallback, primaryColor,
            stageColor, currentRelease, client);
      }

      return k8sTaskHelper.getK8sTaskExecutionResponse(K8sBlueGreenDeployResponse.builder()
                                                           .releaseNumber(currentRelease.getNumber())
                                                           .k8sPodList(podList)
                                                           .primaryServiceName(primaryService.getResourceId().getName())
                                                           .stageServiceName(stageService.getResourceId().getName())
                                                           .stageColor(stageColor)
                                                           .helmChartInfo(helmChartInfo)
                                                           .build(),
          SUCCESS);
    } catch (Exception e) {
      wrapUpLogCallback.saveExecutionLog(e.getMessage(), ERROR, FAILURE);
      releaseHistory.setReleaseStatus(Status.Failed);
      k8sTaskHelperBase.saveReleaseHistoryInConfigMap(
          kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      throw e;
    }
  }

  boolean init(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
        k8sBlueGreenDeployTaskParameters.getK8sClusterConfig(), false);

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());
    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(
        kubernetesConfig, k8sBlueGreenDeployTaskParameters.getReleaseName());
    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<FileData> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
          k8sBlueGreenDeployTaskParameters.getValuesYamlList(), releaseName, kubernetesConfig.getNamespace(),
          executionLogCallback, k8sBlueGreenDeployTaskParameters);

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

    if (k8sBlueGreenDeployTaskParameters.isSkipDryRun()) {
      executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    return k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
  }

  @VisibleForTesting
  boolean prepareForBlueGreen(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      if (isNotTrue(k8sBlueGreenDeployTaskParameters.getSkipVersioningForAllK8sObjects())) {
        markVersionedResources(resources);
      }

      executionLogCallback.saveExecutionLog("Manifests processed. Found following resources: \n"
          + k8sTaskHelperBase.getResourcesInTableFormat(resources));

      List<KubernetesResource> workloads = getWorkloadsForCanaryAndBG(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do  Blue/Green Deployment. Only Deployment, DeploymentConfig (OpenShift) and StatefulSet workloads are supported in Blue/Green workflow type.",
              ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nThere are multiple workloads in the Service Manifests you are deploying. Blue/Green Workflows support a single Deployment, DeploymentConfig (OpenShift) or StatefulSet workload only. To deploy additional workloads in Manifests, annotate them with "
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
        primaryColor = k8sBGBaseHandler.getPrimaryColor(primaryService, kubernetesConfig, executionLogCallback);
        V1Service stageServiceInCluster =
            kubernetesContainerService.getService(kubernetesConfig, stageService.getResourceId().getName());
        if (stageServiceInCluster == null) {
          executionLogCallback.saveExecutionLog(
              "Stage Service [" + stageService.getResourceId().getName() + "] not found in cluster.");
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

      if (k8sBlueGreenDeployTaskParameters.isPruningEnabled()) {
        List<KubernetesResource> resourcesWithoutSkipPruning =
            resources.stream().filter(resource -> !resource.isSkipPruning()).collect(toList());
        currentRelease = releaseHistory.createNewReleaseWithResourceMap(resourcesWithoutSkipPruning);
      } else {
        currentRelease = releaseHistory.createNewRelease(
            resources.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()));
      }

      prePruningInfo = k8sBGBaseHandler.cleanupForBlueGreen(k8sDelegateTaskParams, releaseHistory, executionLogCallback,
          primaryColor, stageColor, currentRelease, client);

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      if (isNotTrue(k8sBlueGreenDeployTaskParameters.getSkipVersioningForAllK8sObjects())) {
        addRevisionNumber(resources, currentRelease.getNumber());
      }
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

  private K8sTaskExecutionResponse getFailureResponse(String errorMessage) {
    K8sBlueGreenDeployResponse k8sBlueGreenDeployResponse = K8sBlueGreenDeployResponse.builder().build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(k8sBlueGreenDeployResponse)
        .errorMessage(errorMessage)
        .build();
  }
}
