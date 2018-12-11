package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;
import static software.wings.delegatetasks.k8s.Utils.applyManifests;
import static software.wings.delegatetasks.k8s.Utils.cleanup;
import static software.wings.delegatetasks.k8s.Utils.describe;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.fetchManifestFiles;
import static software.wings.delegatetasks.k8s.Utils.getCurrentReplicas;
import static software.wings.delegatetasks.k8s.Utils.getLatestRevision;
import static software.wings.delegatetasks.k8s.Utils.getResourcesInTableFormat;
import static software.wings.delegatetasks.k8s.Utils.readManifests;
import static software.wings.delegatetasks.k8s.Utils.renderTemplate;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
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
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCanarySetupTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sCanarySetupResponse;
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
public class K8sCanarySetupTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sCanarySetupTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release release;
  private KubernetesResource managedWorkload;
  private List<KubernetesResource> resources;
  private Integer currentInstances;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sCanarySetupTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sCanarySetupTaskParameters"));
    }

    K8sCanarySetupTaskParameters k8sCanarySetupTaskParameters = (K8sCanarySetupTaskParameters) k8sTaskParameters;

    List<ManifestFile> manifestFiles = fetchManifestFiles(k8sCanarySetupTaskParameters.getK8sDelegateManifestConfig(),
        getLogCallBack(k8sCanarySetupTaskParameters, FetchFiles), gitService, encryptionService);
    if (manifestFiles == null) {
      return getFailureResponse(k8sCanarySetupTaskParameters.getActivityId());
    }
    k8sCanarySetupTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success =
        init(k8sCanarySetupTaskParameters, k8sDelegateTaskParams, getLogCallBack(k8sCanarySetupTaskParameters, Init));
    if (!success) {
      return getFailureResponse(k8sCanarySetupTaskParameters.getActivityId());
    }

    success = prepareForCanary(k8sDelegateTaskParams, getLogCallBack(k8sCanarySetupTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse(k8sCanarySetupTaskParameters.getActivityId());
    }

    success =
        applyManifests(client, resources, k8sDelegateTaskParams, getLogCallBack(k8sCanarySetupTaskParameters, Apply));
    if (!success) {
      return getFailureResponse(k8sCanarySetupTaskParameters.getActivityId());
    }

    release.setManagedWorkload(managedWorkload.getResourceId().cloneInternal());
    release.setManagedWorkloadRevision(
        getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    success = doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
        getLogCallBack(k8sCanarySetupTaskParameters, WaitForSteadyState));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sCanarySetupTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse(k8sCanarySetupTaskParameters.getActivityId());
    }

    wrapUp(k8sDelegateTaskParams, getLogCallBack(k8sCanarySetupTaskParameters, WrapUp));

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sCanarySetupTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    K8sCanarySetupResponse k8sCanarySetupResponse =
        K8sCanarySetupResponse.builder().releaseNumber(release.getNumber()).currentInstances(currentInstances).build();
    k8sCanarySetupResponse.setActivityId(k8sCanarySetupTaskParameters.getActivityId());
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sCanarySetupResponse)
        .build();
  }

  private K8sTaskExecutionResponse getFailureResponse(String activityId) {
    K8sRollingDeployResponse rollingSetupResponse = K8sRollingDeployResponse.builder().build();
    rollingSetupResponse.setActivityId(activityId);
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(rollingSetupResponse)
        .build();
  }

  private ExecutionLogCallback getLogCallBack(K8sCanarySetupTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  private boolean init(K8sCanarySetupTaskParameters k8sCanarySetupTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sCanarySetupTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), k8sCanarySetupTaskParameters.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles = renderTemplate(k8sDelegateTaskParams,
          k8sCanarySetupTaskParameters.getK8sDelegateManifestConfig().getManifestFiles(), executionLogCallback);

      resources = readManifests(manifestFiles, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(e.getMessage(), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(
        "\nManifests [Post template rendering]\n-----------------------------------\n");

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources) + "\n");

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  private boolean prepareForCanary(
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      markVersionedResources(resources, true);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + getResourcesInTableFormat(resources));

      release = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, release.getNumber(), true);
      managedWorkload = getManagedWorkload(resources);
      managedWorkload.setupDeploymentForCanary(release.getNumber());

      executionLogCallback.saveExecutionLog("\nManaged Workload is: " + managedWorkload.getResourceId().kindNameRef());

      cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      if (releaseHistory.getLastSuccessfulRelease() != null
          && releaseHistory.getLastSuccessfulRelease().getManagedWorkload() != null) {
        currentInstances = getCurrentReplicas(
            client, releaseHistory.getLastSuccessfulRelease().getManagedWorkload(), k8sDelegateTaskParams);
        if (currentInstances != null) {
          executionLogCallback.saveExecutionLog("\nCurrent instance count is " + currentInstances);
        }
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

    describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }
}
