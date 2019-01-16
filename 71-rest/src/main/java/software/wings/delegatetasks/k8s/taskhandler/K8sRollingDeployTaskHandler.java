package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogColor.Cyan;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogColor.Yellow;
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

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class K8sRollingDeployTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sRollingDeployTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
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
            k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, FetchFiles), gitService,
            encryptionService);
    if (manifestFiles == null) {
      return getFailureResponse(k8sRollingDeployTaskParameters.getActivityId());
    }
    k8sRollingDeployTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success = init(k8sRollingDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Init));
    if (!success) {
      return getFailureResponse(k8sRollingDeployTaskParameters.getActivityId());
    }

    success = prepareForRolling(
        k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse(k8sRollingDeployTaskParameters.getActivityId());
    }

    success = k8sTaskHelper.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, Apply));
    if (!success) {
      return getFailureResponse(k8sRollingDeployTaskParameters.getActivityId());
    }

    List<K8sPod> podList = Collections.EMPTY_LIST;

    if (managedWorkload == null) {
      k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      release.setManagedWorkload(managedWorkload.getResourceId());
      release.setManagedWorkloadRevision(
          k8sTaskHelper.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

      success = k8sTaskHelper.doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
          k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WaitForSteadyState));
      if (!success) {
        releaseHistory.setReleaseStatus(Status.Failed);
        kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
            k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
        return getFailureResponse(k8sRollingDeployTaskParameters.getActivityId());
      }

      podList =
          k8sTaskHelper.getPodDetails(kubernetesConfig, managedWorkload.getResourceId().getNamespace(), releaseName);
    }

    wrapUp(k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sRollingDeployTaskParameters, WrapUp));

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sRollingDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder().releaseNumber(release.getNumber()).k8sPodList(podList).build();

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(rollingSetupResponse)
        .build();
  }

  private K8sTaskExecutionResponse getFailureResponse(String activityId) {
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

      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          request.getK8sDelegateManifestConfig().getManifestFiles(), request.getValuesYamlList(), executionLogCallback);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    return true;
  }

  private boolean prepareForRolling(
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

      release = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

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
        managedWorkload.addReleaseLabelsInPodSpec(releaseName, release.getNumber());
      }
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
