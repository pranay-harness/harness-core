package io.harness.delegate.task.k8s;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingRollbackDeployRequest implements K8sDeployRequest {
  Integer releaseNumber;
  String releaseName;
  K8sTaskType taskType;
  String commandName;
  Integer timeoutIntervalInMin;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
}
