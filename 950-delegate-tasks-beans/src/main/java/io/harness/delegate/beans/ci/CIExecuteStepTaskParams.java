package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface CIExecuteStepTaskParams extends TaskParameters {
  enum Type { K8, AWS_VM }

  CIExecuteStepTaskParams.Type getType();
}
