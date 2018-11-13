package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedResource;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.StatusCheck;
import static software.wings.delegatetasks.k8s.Utils.applyManifests;
import static software.wings.delegatetasks.k8s.Utils.cleanupForRolling;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getLatestRevision;
import static software.wings.delegatetasks.k8s.Utils.getResourcesInTableFormat;
import static software.wings.delegatetasks.k8s.Utils.readManifests;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.kubectl.Kubectl;
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
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Singleton
public class K8sDeploymentRollingCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingCommandTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  KubernetesResourceId managedResource;
  List<KubernetesResource> resources;

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sDeploymentRollingSetupRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sCommandRequest", "Must be instance of K8sDeploymentRollingSetupRequest"));
    }

    final String namespace = k8sCommandRequest.getK8sClusterConfig().getNamespace();

    K8sDeploymentRollingSetupRequest request = (K8sDeploymentRollingSetupRequest) k8sCommandRequest;

    boolean success = init(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Init));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    cleanupForRolling(client, k8sCommandTaskParams, releaseHistory,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Prepare));

    success = applyManifests(client, resources, namespace, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Apply));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    release.setManagedResource(managedResource);
    release.setManagedResourceRevision(getLatestRevision(client, managedResource, k8sCommandTaskParams));

    success = doStatusCheck(client, managedResource, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), StatusCheck));

    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getInfraMappingId(), releaseHistory.getAsYaml());
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getInfraMappingId(), releaseHistory.getAsYaml());

    return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private boolean init(K8sDeploymentRollingSetupRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getInfraMappingId());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      resources = readManifests(request.getManifestFiles());
    } catch (Exception e) {
      if (e instanceof KubernetesYamlException) {
        executionLogCallback.saveExecutionLog(e.getMessage(), ERROR, CommandExecutionStatus.FAILURE);
      } else {
        executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      }

      return false;
    }

    markVersionedResources(resources);

    executionLogCallback.saveExecutionLog(
        "Manifests processed. Found following resources: \n" + getResourcesInTableFormat(resources));

    managedResource = getManagedResource(resources);
    if (StringUtils.isEmpty(managedResource.getNamespace())) {
      managedResource.setNamespace(kubernetesConfig.getNamespace());
    }

    executionLogCallback.saveExecutionLog("\nManaged Resource is: " + managedResource.kindNameRef());

    release = releaseHistory.createNewRelease(
        resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

    executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

    addRevisionNumber(resources, release.getNumber());

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }
}
