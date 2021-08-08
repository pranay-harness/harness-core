package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sInstanceSyncRequest;
import io.harness.delegate.task.k8s.K8sInstanceSyncResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@NoArgsConstructor
@Slf4j
public class K8sInstanceSyncRequestHandler extends K8sRequestHandler {
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sInstanceSyncRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sInstanceSyncRequest"));
    }
    K8sInstanceSyncRequest k8sInstanceSyncRequest = (K8sInstanceSyncRequest) k8sDeployRequest;
    String namespace = k8sInstanceSyncRequest.getNamespace();
    String releaseName = k8sInstanceSyncRequest.getNamespace();
    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sInstanceSyncRequest.getTimeoutIntervalInMin());
    List<K8sPod> k8sPodList = k8sTaskHelperBase.getPodDetails(
        getKubernetesConfig(k8sInstanceSyncRequest), namespace, releaseName, steadyStateTimeoutInMillis);

    K8sInstanceSyncResponse k8sInstanceSyncResponse = buildK8sInstanceSyncResponse(namespace, releaseName, k8sPodList);
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(k8sInstanceSyncResponse)
        .build();
  }

  private KubernetesConfig getKubernetesConfig(K8sInstanceSyncRequest k8sInstanceSyncRequest) {
    K8sInfraDelegateConfig k8sInfraDelegateConfig = k8sInstanceSyncRequest.getK8sInfraDelegateConfig();
    return containerDeploymentDelegateBaseHelper.createKubernetesConfig(k8sInfraDelegateConfig);
  }

  private K8sInstanceSyncResponse buildK8sInstanceSyncResponse(
      String namespace, String releaseName, List<K8sPod> k8sPodList) {
    return K8sInstanceSyncResponse.builder()
        .k8sPodInfoList(k8sPodList)
        .namespace(namespace)
        .releaseName(releaseName)
        .build();
  }
}
