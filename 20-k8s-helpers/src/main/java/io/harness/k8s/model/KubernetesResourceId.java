package io.harness.k8s.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesResourceId {
  private String kind;
  private String name;
  private String namespace;

  public String kindNameRef() {
    return this.getKind() + "/" + this.getName();
  }

  public KubernetesResourceId cloneInternal() {
    return KubernetesResourceId.builder().kind(this.kind).name(this.name).namespace(this.namespace).build();
  }
}
