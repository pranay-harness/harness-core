package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface CIBuildSetupTaskParams extends TaskParameters {
  enum Type { GCP_K8 }

  CIBuildSetupTaskParams.Type getType();
}
