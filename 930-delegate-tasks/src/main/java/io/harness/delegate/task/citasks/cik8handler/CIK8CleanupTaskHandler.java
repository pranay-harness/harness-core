package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to delete CI build setup pod on a K8 cluster.
 */

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder.getSecretName;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.CICleanupTaskHandler;
import io.harness.logging.AutoLogContext;
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

    if (taskParams.getPodNameList().size() != 1) {
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Only single pod can be cleaned in k8 clean up task")
          .build();
    }

    String podName = taskParams.getPodNameList().get(0);
    try (AutoLogContext ignore1 = new K8LogContext(podName, null, OVERRIDE_ERROR)) {
      try (
          KubernetesClient kubernetesClient = k8sConnectorHelper.createKubernetesClient(taskParams.getK8sConnector())) {
        boolean podsDeleted = deletePods(kubernetesClient, namespace, taskParams.getPodNameList());
        if (podsDeleted) {
          boolean serviceDeleted = deleteServices(kubernetesClient, namespace, taskParams.getServiceNameList());
          boolean secretsDeleted = deleteSecrets(
              kubernetesClient, namespace, taskParams.getPodNameList(), taskParams.getCleanupContainerNames());
          if (podsDeleted && serviceDeleted && secretsDeleted) {
            return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
          } else {
            return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
          }
        } else {
          log.error("Failed to delete pod {}", taskParams.getPodNameList());
          return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
        }
      } catch (Exception ex) {
        log.error("Exception in processing CI K8 delete setup task: {}", taskParams, ex);
        return K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ex.getMessage())
            .build();
      }
    }
  }

  private boolean deletePods(KubernetesClient kubernetesClient, String namespace, List<String> podNameList) {
    boolean isSuccess = true;
    if (isEmpty(podNameList)) {
      log.warn("No pods to delete");
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

  private boolean deleteSecrets(
      KubernetesClient kubernetesClient, String namespace, List<String> podNameList, List<String> containerNames) {
    boolean isSuccess = true;
    if (isEmpty(podNameList)) {
      log.warn("Empty pod list, Failed to delete secrets");
      return false;
    }
    for (String podName : podNameList) {
      String secretName = getSecretName(podName);
      Boolean isDeleted = kubeCtlHandler.deleteSecret(kubernetesClient, namespace, secretName);
      if (isDeleted.equals(Boolean.FALSE)) {
        log.error("Failed to delete secret {}", secretName);
        isSuccess = false;
      }

      if (isNotEmpty(containerNames)) {
        for (String containerName : containerNames) {
          String containerSecretName = format("%s-image-%s", podName, containerName);
          Boolean isDeletedContainerImageSecret =
              kubeCtlHandler.deleteSecret(kubernetesClient, namespace, containerSecretName);
          if (isDeletedContainerImageSecret.equals(Boolean.FALSE)) {
            log.error("Failed to delete secret {}", secretName);
            isSuccess = false;
          }
        }
      }
    }
    return isSuccess;
  }
}
