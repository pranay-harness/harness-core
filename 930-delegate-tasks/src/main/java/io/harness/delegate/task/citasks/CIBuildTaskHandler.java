package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;

public interface CIBuildTaskHandler {
  enum Type { GCP_K8 }

  CIBuildTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(CIBuildSetupTaskParams ciBuildSetupTaskParams);
}
