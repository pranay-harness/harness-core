package io.harness.ngpipeline.executions.beans;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionGraph {
  String rootNodeId;
  Map<String, ExecutionNode> nodeMap;
  Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap;
}
