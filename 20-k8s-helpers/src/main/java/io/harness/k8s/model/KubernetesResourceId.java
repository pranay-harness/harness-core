package io.harness.k8s.model;

import io.harness.exception.WingsException;
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

  public static KubernetesResourceId createKubernetesResourceIdFromKindName(String kindName) {
    String splitArray[] = kindName.trim().split("/");
    if (splitArray.length == 2) {
      return KubernetesResourceId.builder().kind(splitArray[0]).name(splitArray[1]).build();
    } else {
      throw new WingsException("Invalid Kubernetes resource name " + kindName + ". Should be in format Kind/Name");
    }
  }

  public String kindNameRef() {
    return this.getKind() + "/" + this.getName();
  }

  public KubernetesResourceId cloneInternal() {
    return KubernetesResourceId.builder().kind(this.kind).name(this.name).namespace(this.namespace).build();
  }
}
