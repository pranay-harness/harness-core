package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
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

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sRollingDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sRollingDeployTaskParameters"));
    }

    K8sRollingDeployTaskParameters request = (K8sRollingDeployTaskParameters) k8sTaskParameters;

    List<ManifestFile> manifestFiles = k8sTaskHelper.fetchManifestFiles(
        request.getK8sDelegateManifestConfig(), getLogCallBack(request, FetchFiles), gitService, encryptionService);
    if (manifestFiles == null) {
      return getFailureResponse(request.getActivityId());
    }
    request.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success = init(request, k8sDelegateTaskParams, getLogCallBack(request, Init));
    if (!success) {
      return getFailureResponse(request.getActivityId());
    }

    success = prepareForRolling(k8sDelegateTaskParams, getLogCallBack(request, Prepare));
    if (!success) {
      return getFailureResponse(request.getActivityId());
    }

    success = k8sTaskHelper.applyManifests(client, resources, k8sDelegateTaskParams, getLogCallBack(request, Apply));
    if (!success) {
      return getFailureResponse(request.getActivityId());
    }

    release.setManagedWorkload(managedWorkload.getResourceId());
    release.setManagedWorkloadRevision(
        k8sTaskHelper.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    success = k8sTaskHelper.doStatusCheck(
        client, managedWorkload.getResourceId(), k8sDelegateTaskParams, getLogCallBack(request, WaitForSteadyState));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());
      return getFailureResponse(request.getActivityId());
    }

    wrapUp(k8sDelegateTaskParams, getLogCallBack(request, WrapUp));

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());

    K8sRollingDeployResponse rollingSetupResponse =
        K8sRollingDeployResponse.builder().releaseNumber(release.getNumber()).build();
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

  private ExecutionLogCallback getLogCallBack(K8sRollingDeployTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  private boolean init(K8sRollingDeployTaskParameters request, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(
          k8sDelegateTaskParams, request.getK8sDelegateManifestConfig().getManifestFiles(), executionLogCallback);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(
        "\nManifests [Post template rendering]\n-----------------------------------\n");

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources) + "\n");

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  private boolean prepareForRolling(
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      markVersionedResources(resources);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      managedWorkload = getManagedWorkload(resources);

      executionLogCallback.saveExecutionLog("\nManaged Workload is: " + managedWorkload.getResourceId().kindNameRef());

      release = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

      k8sTaskHelper.cleanup(client, k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, release.getNumber());

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
