package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface ExecuteCommandTaskParams extends TaskParameters {
  enum Type { GCP_K8 }
  ExecuteCommandTaskParams.Type getType();
}
