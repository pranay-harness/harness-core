package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to execute a command or list of commands on a pod's container in a kubernetes cluster.
 */

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.esotericsoftware.kryo.NotNull;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.K8ExecCommandParams;
import io.harness.delegate.beans.ci.K8ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.ExecuteCommandTaskHandler;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Singleton
@Slf4j
public class K8ExecuteCommandTaskHandler implements ExecuteCommandTaskHandler {
  @NotNull private ExecuteCommandTaskHandler.Type type = ExecuteCommandTaskHandler.Type.K8;
  @Inject private K8CommandExecutor k8CommandExecutor;
  @Inject private K8sConnectorHelper k8sConnectorHelper;

  @Override
  public ExecuteCommandTaskHandler.Type getType() {
    return type;
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(ExecuteCommandTaskParams executeCommandTaskParams) {
    K8ExecuteCommandTaskParams k8ExecuteCommandTaskParams = (K8ExecuteCommandTaskParams) executeCommandTaskParams;
    K8ExecCommandParams k8ExecCommandParams = k8ExecuteCommandTaskParams.getK8ExecCommandParams();
    String podName = k8ExecCommandParams.getPodName();
    String containerName = k8ExecCommandParams.getContainerName();
    List<String> commands = k8ExecCommandParams.getCommands();

    K8sTaskExecutionResponse result;
    try (AutoLogContext ignore = new K8LogContext(podName, containerName, OVERRIDE_ERROR)) {
      try (DefaultKubernetesClient kubernetesClient =
               k8sConnectorHelper.getDefaultKubernetesClient(k8ExecuteCommandTaskParams.getK8sConnector())) {
        ExecCommandStatus status = k8CommandExecutor.executeCommand(kubernetesClient, k8ExecCommandParams);
        if (status == ExecCommandStatus.SUCCESS) {
          log.info("Successfully executed commands {} on container {} of pod {}", commands, containerName, podName);
          result = K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
        } else {
          log.info("Failed to execute commands {} on container {} of pod {} with status {}", commands, containerName,
              podName, status);
          result = K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
        }

      } catch (Exception e) {
        log.error("Exception in processing CI execute command task: {}", commands, e);
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorMessage(e.getMessage())
                     .build();
      }
    }
    return result;
  }
}
