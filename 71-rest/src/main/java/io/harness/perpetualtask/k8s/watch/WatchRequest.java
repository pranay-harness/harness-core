package io.harness.perpetualtask.k8s.watch;

import lombok.Builder;
import lombok.Value;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

@Value
@Builder
public class WatchRequest {
  private K8sClusterConfig k8sClusterConfig;
  private String k8sResourceKind;
  private String namespace;
}
