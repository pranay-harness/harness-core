package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

public interface CIInitializeTaskHandler {
  enum Type { GCP_K8, AWS_VM }

  CIInitializeTaskHandler.Type getType();

  CITaskExecutionResponse executeTaskInternal(
      CIInitializeTaskParams ciInitializeTaskParams, ILogStreamingTaskClient logStreamingTaskClient);
}
