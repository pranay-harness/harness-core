package software.wings.delegatetasks.k8s.taskhandler;

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
import static io.harness.k8s.manifest.ManifestHelper.getCustomResourceDefinitionWorkloads;
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

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

import io.harness.beans.FileData;
import io.harness.delegate.k8s.K8sRollingBaseHandler;
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

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
public class K8sRollingDeployTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject K8sRollingBaseHandler k8sRollingBaseHandler;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  List<KubernetesResource> managedWorkloads;
  List<KubernetesResource> customWorkloads;
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
    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    boolean success = k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(
        k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, FetchFiles), steadyStateTimeoutInMillis);
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sRollingDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepareForRolling(k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Prepare),
        k8sRollingDeployTaskParameters.isInCanaryWorkflow(),
        k8sRollingDeployTaskParameters.getSkipVersioningForAllK8sObjects());
    if (!success) {
      return getFailureResponse();
    }

    List<KubernetesResource> allWorkloads = ListUtils.union(managedWorkloads, customWorkloads);
    List<K8sPod> existingPodList =
        k8sRollingBaseHandler.getPods(k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled(),
            steadyStateTimeoutInMillis, allWorkloads, kubernetesConfig, releaseName);

    success = k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Apply), true);
    if (!success) {
      return getFailureResponse();
    }

    if (isEmpty(managedWorkloads) && isEmpty(customWorkloads)) {
      k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      k8sRollingBaseHandler.setManagedWorkloadsInRelease(k8sDelegateTaskParams, managedWorkloads, release, client);
      k8sRollingBaseHandler.setCustomWorkloadsInRelease(customWorkloads, release);

      k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, k8sRollingDeployTaskParameters.getReleaseName(),
          releaseHistory.getAsYaml(), !customWorkloads.isEmpty(),
          k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled());

      List<KubernetesResourceId> managedWorkloadKubernetesResourceIds =
          managedWorkloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());
      ExecutionLogCallback executionLogCallback =
          k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState);

      success = k8sTaskHelperBase.doStatusCheckForAllResources(client, managedWorkloadKubernetesResourceIds,
          k8sDelegateTaskParams, kubernetesConfig.getNamespace(), executionLogCallback, customWorkloads.isEmpty());

      boolean customWorkloadsStatusSuccess = k8sTaskHelperBase.doStatusCheckForAllCustomResources(
          client, customWorkloads, k8sDelegateTaskParams, executionLogCallback, true, steadyStateTimeoutInMillis);

      // We have to update the DeploymentConfig revision again as the rollout history command sometimes gives the older
      // revision. There seems to be delay in handling of the DeploymentConfig where it still gives older revision even
      // after the apply command has successfully run
      k8sRollingBaseHandler.updateDeploymentConfigRevision(k8sDelegateTaskParams, release, client);

      if (!success || !customWorkloadsStatusSuccess) {
        releaseHistory.setReleaseStatus(Status.Failed);
        k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, k8sRollingDeployTaskParameters.getReleaseName(),
            releaseHistory.getAsYaml(), !customWorkloads.isEmpty(),
            k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled());
        return getFailureResponse();
      }
    }

    HelmChartInfo helmChartInfo = k8sTaskHelper.getHelmChartDetails(
        k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory);

    k8sRollingBaseHandler.wrapUp(
        k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WrapUp), client);

    releaseHistory.setReleaseStatus(Status.Succeeded);
    k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, k8sRollingDeployTaskParameters.getReleaseName(),
        releaseHistory.getAsYaml(), !customWorkloads.isEmpty(),
        k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled());

    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder()
            .releaseNumber(release.getNumber())
            .k8sPodList(k8sRollingBaseHandler.tagNewPods(
                k8sRollingBaseHandler.getPods(k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled(),
                    steadyStateTimeoutInMillis, allWorkloads, kubernetesConfig, releaseName),
                existingPodList))
            .loadBalancer(k8sRollingDeployTaskParameters.isDeprecateFabric8Enabled()
                    ? k8sTaskHelperBase.getLoadBalancerEndpoint(kubernetesConfig, resources)
                    : k8sTaskHelperBase.getLoadBalancerEndpointFabric8(kubernetesConfig, resources))
            .helmChartInfo(helmChartInfo)
            .build();

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(rollingSetupResponse)
        .build();
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
    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig(), false);
    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());
    try {
      String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryData(
          kubernetesConfig, request.getReleaseName(), request.isDeprecateFabric8Enabled());
      releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                                 : ReleaseHistory.createFromData(releaseHistoryData);

      k8sTaskHelperBase.deleteSkippedManifestFiles(manifestFilesDirectory, executionLogCallback);

      List<FileData> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          request.getK8sDelegateManifestConfig(), manifestFilesDirectory, request.getValuesYamlList(), releaseName,
          kubernetesConfig.getNamespace(), executionLogCallback, request);

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

  private boolean prepareForRolling(K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback,
      boolean inCanaryWorkflow, Boolean skipVersioningForAllK8sObjects) {
    try {
      managedWorkloads = getWorkloads(resources);
      if (isNotEmpty(managedWorkloads) && isNotTrue(skipVersioningForAllK8sObjects)) {
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

      customWorkloads = getCustomResourceDefinitionWorkloads(resources);

      if (isEmpty(managedWorkloads) && isEmpty(customWorkloads)) {
        executionLogCallback.saveExecutionLog(color("\nNo Managed Workload found.", Yellow, Bold));
      } else {
        executionLogCallback.saveExecutionLog(color("\nFound following Managed Workloads: \n", Cyan, Bold)
            + k8sTaskHelperBase.getResourcesInTableFormat(ListUtils.union(managedWorkloads, customWorkloads)));

        k8sTaskHelperBase.checkSteadyStateCondition(customWorkloads);

        executionLogCallback.saveExecutionLog("\nVersioning resources.");
        if (isNotTrue(skipVersioningForAllK8sObjects)) {
          addRevisionNumber(resources, release.getNumber());
        }

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
