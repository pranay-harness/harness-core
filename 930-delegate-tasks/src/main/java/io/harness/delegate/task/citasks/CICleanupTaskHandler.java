package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;

public interface CICleanupTaskHandler {
  enum Type { GCP_K8 }

  CICleanupTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams);
}
