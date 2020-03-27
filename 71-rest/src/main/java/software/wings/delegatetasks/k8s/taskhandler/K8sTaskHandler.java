package software.wings.delegatetasks.k8s.taskhandler;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class K8sTaskHandler {
  @Inject protected DelegateLogService delegateLogService;

  public K8sTaskExecutionResponse executeTask(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) {
    K8sTaskExecutionResponse result;
    try {
      result = executeTaskInternal(k8STaskParameters, k8SDelegateTaskParams);
    } catch (IOException ex) {
      logError(k8STaskParameters, ex);
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Could not complete k8s task due to IO exception")
                   .build();
    } catch (TimeoutException ex) {
      logError(k8STaskParameters, ex);
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Timed out while waiting for k8s task to complete")
                   .build();
    } catch (InterruptedException ex) {
      logError(k8STaskParameters, ex);
      Thread.currentThread().interrupt();
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Interrupted while waiting for k8s task to complete")
                   .build();
    } catch (WingsException ex) {
      logError(k8STaskParameters, ex);
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage(ExceptionUtils.getMessage(ex))
                   .build();
    } catch (Exception ex) {
      logError(k8STaskParameters, ex);
      result = K8sTaskExecutionResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .errorMessage("Failed to complete k8s task")
                   .build();
    }
    return result;
  }

  protected abstract K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) throws Exception;

  private void logError(K8sTaskParameters taskParameters, Throwable ex) {
    logger.error("Exception in processing K8s task [{}]",
        taskParameters.getCommandName() + ":" + taskParameters.getCommandType(), ex);
  }
}
