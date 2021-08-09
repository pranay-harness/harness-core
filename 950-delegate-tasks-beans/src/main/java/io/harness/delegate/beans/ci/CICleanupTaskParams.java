package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface CICleanupTaskParams extends TaskParameters {
  enum Type { GCP_K8 }

  CICleanupTaskParams.Type getType();
}
