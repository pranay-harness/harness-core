package io.harness.cdng.pipeline.executions.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ExecutionGraph {
  String rootNodeId;
  Map<String, ExecutionNode> nodeMap;
  Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap;
}
