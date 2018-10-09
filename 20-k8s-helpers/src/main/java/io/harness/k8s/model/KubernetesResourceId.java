package io.harness.k8s.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesResourceId {
  private String kind;
  private String name;
  private String namespace;
}
