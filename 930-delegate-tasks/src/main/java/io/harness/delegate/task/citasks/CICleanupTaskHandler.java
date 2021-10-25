package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;

public interface CICleanupTaskHandler {
  enum Type { GCP_K8, AWS_VM }

  CICleanupTaskHandler.Type getType();

  CITaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams);
}
