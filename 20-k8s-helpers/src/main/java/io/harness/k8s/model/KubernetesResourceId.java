package io.harness.k8s.model;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    if (splitArray.length == 3) {
      return KubernetesResourceId.builder().namespace(splitArray[0]).kind(splitArray[1]).name(splitArray[2]).build();
    } else if (splitArray.length == 2) {
      return KubernetesResourceId.builder().kind(splitArray[0]).name(splitArray[1]).build();
    } else {
      throw new WingsException("Invalid Kubernetes resource name " + kindName + ". Should be in format Kind/Name");
    }
  }

  public static List<KubernetesResourceId> createKubernetesResourceIdsFromKindName(String resources) {
    String resourceArray[] = resources.trim().split(",");

    List<KubernetesResourceId> result = new ArrayList<>();

    for (String resource : resourceArray) {
      result.add(createKubernetesResourceIdFromKindName(resource));
    }

    return result;
  }

  public String kindNameRef() {
    return this.getKind() + "/" + this.getName();
  }

  public String namespaceKindNameRef() {
    if (!isBlank(this.getNamespace())) {
      return this.getNamespace() + this.kindNameRef();
    }
    return this.kindNameRef();
  }

  public KubernetesResourceId cloneInternal() {
    return KubernetesResourceId.builder().kind(this.kind).name(this.name).namespace(this.namespace).build();
  }
}
