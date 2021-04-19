package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sNGTaskResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public abstract class K8sRequestHandler {
  @Inject protected K8sTaskHelperBase k8sTaskHelperBase;

  private LogCallback currentLogCallback;
  private CommandUnitsProgress commandUnitsProgress;
  private ILogStreamingTaskClient logStreamingTaskClient;

  public K8sDeployResponse executeTask(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8SDelegateTaskParams,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    K8sDeployResponse result;
    if (isErrorFrameworkSupported()) {
      this.commandUnitsProgress = commandUnitsProgress;
      this.logStreamingTaskClient = logStreamingTaskClient;
      return executeTaskInternal(k8sDeployRequest, k8SDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
    } else {
      try {
        result =
            executeTaskInternal(k8sDeployRequest, k8SDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
      } catch (IOException ex) {
        logError(k8sDeployRequest, ex);
        result = K8sDeployResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .k8sNGTaskResponse(getTaskResponseOnFailure())
                     .errorMessage("Could not complete k8s task due to IO exception")
                     .build();
      } catch (TimeoutException ex) {
        logError(k8sDeployRequest, ex);
        result = K8sDeployResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .k8sNGTaskResponse(getTaskResponseOnFailure())
                     .errorMessage("Timed out while waiting for k8s task to complete")
                     .build();
      } catch (InterruptedException ex) {
        logError(k8sDeployRequest, ex);
        Thread.currentThread().interrupt();
        result = K8sDeployResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .k8sNGTaskResponse(getTaskResponseOnFailure())
                     .errorMessage("Interrupted while waiting for k8s task to complete")
                     .build();
      } catch (WingsException ex) {
        logError(k8sDeployRequest, ex);
        result = K8sDeployResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .k8sNGTaskResponse(getTaskResponseOnFailure())
                     .errorMessage(ExceptionUtils.getMessage(ex))
                     .build();
      } catch (Exception ex) {
        logError(k8sDeployRequest, ex);
        result = K8sDeployResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .k8sNGTaskResponse(getTaskResponseOnFailure())
                     .errorMessage("Failed to complete K8s task. Please check execution logs.")
                     .build();
      }
    }

    return result;
  }

  public boolean isErrorFrameworkSupported() {
    return false;
  }

  public void handleTaskFailure(K8sDeployRequest k8sDeployRequest, Exception exception) {
    currentLogCallback.saveExecutionLog(ExceptionUtils.getMessage(exception), ERROR);
    currentLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
    onTaskFailed(k8sDeployRequest);
  }

  public LogCallback getCurrentLogCallback() {
    return currentLogCallback;
  }

  protected abstract K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception;

  protected void startNewCommandUnit(String commandUnitName, boolean shouldOpenStream) {
    currentLogCallback = k8sTaskHelperBase.getLogCallback(
        logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }

  protected void onTaskFailed(K8sDeployRequest deployRequest) {}

  protected K8sDeployResponse getGenericFailureResponse(K8sNGTaskResponse taskResponse) {
    return K8sDeployResponse.builder()
        .commandExecutionStatus(FAILURE)
        .k8sNGTaskResponse(taskResponse)
        .errorMessage("Failed to complete K8s task. Please check execution logs.")
        .build();
  }

  protected K8sNGTaskResponse getTaskResponseOnFailure() {
    return null;
  }

  private void logError(K8sDeployRequest k8sDeployRequest, Throwable ex) {
    log.error("Exception in processing K8s task [{}]",
        k8sDeployRequest.getCommandName() + ":" + k8sDeployRequest.getTaskType(), ex);
  }
}
