package io.harness.argo.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterResourceTree {
  List<Node> nodes;
  List<Host> hosts;

  class Node {
    Map<String, Object> data;
  }
  class Host {
    Map<String, Object> data;
  }
}
