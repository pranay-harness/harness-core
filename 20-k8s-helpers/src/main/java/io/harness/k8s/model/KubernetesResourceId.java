package io.harness.k8s.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubernetesResourceId {
  private String kind;
  private String name;
  private String namespace;
  private boolean versioned;

  public String kindNameRef() {
    return this.getKind() + "/" + this.getName();
  }

  public KubernetesResourceId cloneInternal() {
    return KubernetesResourceId.builder().kind(this.kind).name(this.name).namespace(this.namespace).build();
  }
}
