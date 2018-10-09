package io.harness.k8s.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesResource {
  private KubernetesResourceId resourceId;
  private Object value;
}
