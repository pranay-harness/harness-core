package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface CIInitializeTaskParams extends TaskParameters {
  enum Type { GCP_K8, AWS_VM }

  CIInitializeTaskParams.Type getType();
}
