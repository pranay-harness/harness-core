package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeList {
  Map<String, List<Edge>> groupedEdges;
  List<Edge> edges;

  @Value
  @Builder
  public static class Edge {
    public enum EdgeType { SIBLING, CHILD }
    String toNodeId;
    EdgeType edgeType;
  }
}
