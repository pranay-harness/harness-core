package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class ExecutionGraph {
  String rootNodeId;
  Map<String, ExecutionNode> nodeMap;
  Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap;
  RepresentationStrategy representationStrategy = RepresentationStrategy.CAMELCASE;
}
