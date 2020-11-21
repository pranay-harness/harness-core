package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to delete CI build setup pod on a K8 cluster.
 */

import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.CICleanupTaskHandler;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIK8CleanupTaskHandler implements CICleanupTaskHandler {
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private K8sConnectorHelper k8sConnectorHelper;

  @NotNull private CICleanupTaskHandler.Type type = CICleanupTaskHandler.Type.GCP_K8;

  @Override
  public CICleanupTaskHandler.Type getType() {
    return type;
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams) {
    CIK8CleanupTaskParams taskParams = (CIK8CleanupTaskParams) ciCleanupTaskParams;
    String namespace = taskParams.getNamespace();

    K8sTaskExecutionResponse result;
    try (KubernetesClient kubernetesClient = k8sConnectorHelper.createKubernetesClient(taskParams.getK8sConnector());) {
      boolean podsDeleted = deletePods(kubernetesClient, namespace, taskParams.getPodNameList());
      boolean serviceDeleted = deleteServices(kubernetesClient, namespace, taskParams.getServiceNameList());
      if (podsDeleted && serviceDeleted) {
        result = K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
      } else {
        result = K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
      }
    } catch (Exception ex) {
      log.error("Exception in processing CI K8 delete setup task: {}", taskParams, ex);
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage(ex.getMessage())
                   .build();
    }
    return result;
  }

  private boolean deletePods(KubernetesClient kubernetesClient, String namespace, List<String> podNameList) {
    boolean isSuccess = true;
    if (podNameList == null) {
      return isSuccess;
    }
    for (String podName : podNameList) {
      Boolean isDeleted = kubeCtlHandler.deletePod(kubernetesClient, podName, namespace);
      if (isDeleted.equals(Boolean.FALSE)) {
        log.error("Failed to delete pod {}", podName);
        isSuccess = false;
      }
    }
    return isSuccess;
  }

  private boolean deleteServices(KubernetesClient kubernetesClient, String namespace, List<String> serviceNameList) {
    boolean isSuccess = true;
    if (serviceNameList == null) {
      return isSuccess;
    }
    for (String serviceName : serviceNameList) {
      Boolean isDeleted = kubeCtlHandler.deleteService(kubernetesClient, namespace, serviceName);
      if (isDeleted.equals(Boolean.FALSE)) {
        log.error("Failed to delete service {}", serviceName);
        isSuccess = false;
      }
    }
    return isSuccess;
  }
}
