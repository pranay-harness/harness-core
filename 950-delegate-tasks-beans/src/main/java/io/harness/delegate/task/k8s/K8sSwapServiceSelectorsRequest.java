package io.harness.delegate.task.k8s;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sSwapServiceSelectorsRequest implements K8sDeployRequest {
  String commandName;
  K8sTaskType taskType;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  boolean deprecateFabric8Enabled;
  Integer timeoutIntervalInMin;
  String accountId;
  String service1;
  String service2;
}
