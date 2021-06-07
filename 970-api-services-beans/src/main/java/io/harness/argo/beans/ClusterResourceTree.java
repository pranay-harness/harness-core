package io.harness.argo.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterResourceTree {
  List<Node> nodes;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Node {
    private String name;
    private String uid;
    private String kind;
    private String namespace;
    private List<Node> parentRefs;

    public String getParentRef() {
      if (this.parentRefs != null) {
        return  parentRefs.get(0).getUid();
      }
      return "";
    }
  }
}
