package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedResource;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.StatusCheck;
import static software.wings.delegatetasks.k8s.Utils.applyManifests;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getRevisionNumber;
import static software.wings.delegatetasks.k8s.Utils.readManifests;

import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;

import java.util.List;

@NoArgsConstructor
@Singleton
public class K8sDeploymentRollingCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingCommandTaskHandler.class);

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sDeploymentRollingSetupRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sCommandRequest", "Must be instance of K8sDeploymentRollingSetupRequest"));
    }

    final String namespace = k8sCommandRequest.getK8sClusterConfig().getNamespace();

    K8sDeploymentRollingSetupRequest request = (K8sDeploymentRollingSetupRequest) k8sCommandRequest;

    List<KubernetesResource> resources = readManifests(request.getManifestFiles(), getRevisionNumber(),
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Init));

    KubernetesResourceId managedResource = getManagedResource(resources);
    if (StringUtils.isEmpty(managedResource.getNamespace())) {
      managedResource.setNamespace(namespace);
    }

    Kubectl client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    boolean success = applyManifests(client, resources, namespace, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Apply));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = doStatusCheck(client, managedResource, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), StatusCheck));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }
}
