package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;

public interface CIExecuteStepTaskHandler {
  enum Type { K8, VM }

  CIExecuteStepTaskHandler.Type getType();

  CITaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams, String taskId);
}
