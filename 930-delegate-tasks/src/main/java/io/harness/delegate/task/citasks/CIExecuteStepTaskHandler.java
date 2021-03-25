package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;

public interface CIExecuteStepTaskHandler {
  enum Type { K8 }

  CIExecuteStepTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams);
}
