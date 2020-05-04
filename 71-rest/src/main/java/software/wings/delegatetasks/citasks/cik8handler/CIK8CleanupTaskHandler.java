package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Delegate task handler to delete CI build setup pod on a K8 cluster.
 */

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.CICleanupTaskParams;
import software.wings.beans.ci.CIK8CleanupTaskParams;
import software.wings.delegatetasks.citasks.CICleanupTaskHandler;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;

import javax.validation.constraints.NotNull;

@Slf4j
public class CIK8CleanupTaskHandler implements CICleanupTaskHandler {
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private KubernetesHelperService kubernetesHelperService;

  @NotNull private CICleanupTaskHandler.Type type = CICleanupTaskHandler.Type.GCP_K8;

  @Override
  public CICleanupTaskHandler.Type getType() {
    return type;
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams) {
    CIK8CleanupTaskParams taskParams = (CIK8CleanupTaskParams) ciCleanupTaskParams;
    String podName = taskParams.getPodName();
    String namespace = taskParams.getNamespace();

    K8sTaskExecutionResponse result;
    try (AutoLogContext ignore1 = new K8LogContext(podName, null, OVERRIDE_ERROR)) {
      try {
        KubernetesClient kubernetesClient = createKubernetesClient(taskParams);
        Boolean isDeleted = kubeCtlHandler.deletePod(kubernetesClient, podName, namespace);
        if (isDeleted) {
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
                       .build();
        } else {
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                       .build();
        }
      } catch (Exception ex) {
        logger.error("Exception in processing CI K8 delete setup task: {}", taskParams, ex);
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                     .errorMessage(ex.getMessage())
                     .build();
      }
    }
    return result;
  }

  private KubernetesClient createKubernetesClient(CIK8CleanupTaskParams cik8DeleteSetupTaskParams) {
    return kubernetesHelperService.getKubernetesClient(
        cik8DeleteSetupTaskParams.getKubernetesConfig(), cik8DeleteSetupTaskParams.getEncryptionDetails());
  }
}
