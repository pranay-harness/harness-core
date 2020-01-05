package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.K8sPod;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.KubernetesConfig;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.List;

@NoArgsConstructor
@Slf4j
public class K8sInstanceSyncTaskHandler extends K8sTaskHandler {
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sInstanceSyncTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sInstanceSyncTaskParameters"));
    }

    K8sInstanceSyncTaskParameters k8sInstanceSyncTaskParameters = (K8sInstanceSyncTaskParameters) k8sTaskParameters;

    KubernetesConfig kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sInstanceSyncTaskParameters.getK8sClusterConfig());

    List<K8sPod> k8sPodList = k8sTaskHelper.getPodDetails(
        kubernetesConfig, k8sInstanceSyncTaskParameters.getNamespace(), k8sInstanceSyncTaskParameters.getReleaseName());

    K8sInstanceSyncResponse k8sInstanceSyncResponse =
        K8sInstanceSyncResponse.builder().k8sPodInfoList(k8sPodList).build();

    return k8sTaskHelper.getK8sTaskExecutionResponse(k8sInstanceSyncResponse, (k8sPodList != null) ? SUCCESS : FAILURE);
  }
}
