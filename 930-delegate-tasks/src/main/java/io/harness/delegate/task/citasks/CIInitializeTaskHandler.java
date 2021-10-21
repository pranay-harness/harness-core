package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

public interface CIInitializeTaskHandler {
  enum Type { GCP_K8 }

  CIInitializeTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(
      CIInitializeTaskParams ciInitializeTaskParams, ILogStreamingTaskClient logStreamingTaskClient);
}
