/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.CommandUnitProgress;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sNGTaskResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public abstract class K8sRequestHandler {
  public K8sDeployResponse executeTask(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8SDelegateTaskParams,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    K8sDeployResponse result;
    try {
      result =
          executeTaskInternal(k8sDeployRequest, k8SDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
    } catch (IOException ex) {
      logError(k8sDeployRequest, ex);
      closeOpenCommandUnits(commandUnitsProgress, logStreamingTaskClient, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .k8sNGTaskResponse(getTaskResponseOnFailure())
                   .errorMessage("Could not complete k8s task due to IO exception")
                   .build();
    } catch (TimeoutException ex) {
      logError(k8sDeployRequest, ex);
      closeOpenCommandUnits(commandUnitsProgress, logStreamingTaskClient, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .k8sNGTaskResponse(getTaskResponseOnFailure())
                   .errorMessage("Timed out while waiting for k8s task to complete")
                   .build();
    } catch (InterruptedException ex) {
      logError(k8sDeployRequest, ex);
      closeOpenCommandUnits(commandUnitsProgress, logStreamingTaskClient, ex);
      Thread.currentThread().interrupt();
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .k8sNGTaskResponse(getTaskResponseOnFailure())
                   .errorMessage("Interrupted while waiting for k8s task to complete")
                   .build();
    } catch (WingsException ex) {
      logError(k8sDeployRequest, ex);
      closeOpenCommandUnits(commandUnitsProgress, logStreamingTaskClient, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .k8sNGTaskResponse(getTaskResponseOnFailure())
                   .errorMessage(ExceptionUtils.getMessage(ex))
                   .build();
    } catch (Exception ex) {
      logError(k8sDeployRequest, ex);
      closeOpenCommandUnits(commandUnitsProgress, logStreamingTaskClient, ex);
      result = K8sDeployResponse.builder()
                   .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                   .k8sNGTaskResponse(getTaskResponseOnFailure())
                   .errorMessage("Failed to complete K8s task. Please check execution logs.")
                   .build();
    }
    return result;
  }

  protected abstract K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception;

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

  private void closeOpenCommandUnits(
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient, Throwable throwable) {
    try {
      LinkedHashMap<String, CommandUnitProgress> commandUnitProgressMap =
          commandUnitsProgress.getCommandUnitProgressMap();
      if (EmptyPredicate.isNotEmpty(commandUnitProgressMap)) {
        for (Map.Entry<String, CommandUnitProgress> entry : commandUnitProgressMap.entrySet()) {
          String commandUnitName = entry.getKey();
          CommandUnitProgress progress = entry.getValue();
          if (CommandExecutionStatus.RUNNING == progress.getStatus()) {
            LogCallback logCallback =
                new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, false, commandUnitsProgress);
            logCallback.saveExecutionLog(
                String.format("Failed: [%s].", ExceptionUtils.getMessage(throwable)), LogLevel.ERROR, FAILURE);
          }
        }
      }
    } catch (Exception ex) {
      log.error("Exception while closing command units", ex);
    }
  }

  private void logError(K8sDeployRequest k8sDeployRequest, Throwable ex) {
    log.error("Exception in processing K8s task [{}]",
        k8sDeployRequest.getCommandName() + ":" + k8sDeployRequest.getTaskType(), ex);
  }
}
