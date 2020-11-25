package io.harness.delegate.k8s;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class K8sRequestHandler {
  public K8sDeployResponse executeTask(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8SDelegateTaskParams,
      ILogStreamingTaskClient logStreamingTaskClient) {
    K8sDeployResponse result;
    try {
      result = executeTaskInternal(k8sDeployRequest, k8SDelegateTaskParams, logStreamingTaskClient);
    } catch (IOException ex) {
      logError(k8sDeployRequest, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Could not complete k8s task due to IO exception")
                   .build();
    } catch (TimeoutException ex) {
      logError(k8sDeployRequest, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Timed out while waiting for k8s task to complete")
                   .build();
    } catch (InterruptedException ex) {
      logError(k8sDeployRequest, ex);
      Thread.currentThread().interrupt();
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Interrupted while waiting for k8s task to complete")
                   .build();
    } catch (WingsException ex) {
      logError(k8sDeployRequest, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage(ExceptionUtils.getMessage(ex))
                   .build();
    } catch (Exception ex) {
      logError(k8sDeployRequest, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Failed to complete k8s task")
                   .build();
    }
    return result;
  }

  protected abstract K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient) throws Exception;

  private void logError(K8sDeployRequest k8sDeployRequest, Throwable ex) {
    log.error("Exception in processing K8s task [{}]",
        k8sDeployRequest.getCommandName() + ":" + k8sDeployRequest.getTaskType(), ex);
  }
}
