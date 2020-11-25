package io.harness.delegate.task.k8s;

import io.harness.delegate.task.TaskParameters;

public interface K8sDeployRequest extends TaskParameters {
  K8sTaskType getTaskType();
  String getCommandName();
  K8sInfraDelegateConfig getK8sInfraDelegateConfig();
  ManifestDelegateConfig getManifestDelegateConfig();
  Integer getTimeoutIntervalInMin();
  boolean isDeprecateFabric8Enabled();
}
