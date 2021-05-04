package io.harness.ngpipeline.executions.mapper;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EdgeList;
import io.harness.beans.ExecutionGraph;
import io.harness.beans.ExecutionGraph.ExecutionGraphBuilder;
import io.harness.beans.ExecutionNode;
import io.harness.beans.ExecutionNodeAdjacencyList;
import io.harness.dto.GraphVertexDTO;
import io.harness.dto.OrchestrationAdjacencyListDTO;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.execution.ExecutionStatus;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

@OwnedBy(CDC)
public class ExecutionGraphMapper {
  private ExecutionGraphMapper() {}

  @NonNull
  public static ExecutionGraph toExecutionGraph(@NonNull OrchestrationGraphDTO orchestrationGraph) {
    ExecutionGraphBuilder executionGraphBuilder =
        ExecutionGraph.builder().rootNodeId(orchestrationGraph.getRootNodeIds().get(0));

    OrchestrationAdjacencyListDTO adjacencyList = orchestrationGraph.getAdjacencyList();

    Map<String, ExecutionNode> nodeMap = new HashMap<>();
    adjacencyList.getGraphVertexMap().forEach((key, value) -> nodeMap.put(key, toExecutionNode(value)));

    Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap = new HashMap<>();
    adjacencyList.getAdjacencyMap().forEach(
        (key, value) -> nodeAdjacencyListMap.put(key, toExecutionNodeAdjacencyList(value)));

    return executionGraphBuilder.nodeMap(nodeMap).nodeAdjacencyListMap(nodeAdjacencyListMap).build();
  }

  @NonNull
  static ExecutionNode toExecutionNode(@NonNull GraphVertexDTO graphVertex) {
    return ExecutionNode.builder()
        .endTs(graphVertex.getEndTs())
        .failureInfo(graphVertex.getFailureInfo())
        .skipInfo(graphVertex.getSkipInfo())
        .nodeRunInfo(graphVertex.getNodeRunInfo())
        .name(graphVertex.getName())
        .startTs(graphVertex.getStartTs())
        .status(ExecutionStatus.getExecutionStatus(graphVertex.getStatus()))
        .stepType(graphVertex.getStepType())
        .uuid(graphVertex.getUuid())
        .executableResponses(graphVertex.getExecutableResponses())
        .setupId(graphVertex.getPlanNodeId())
        .build();
  }

  @NonNull
  static ExecutionNodeAdjacencyList toExecutionNodeAdjacencyList(@NonNull EdgeList edgeList) {
    return ExecutionNodeAdjacencyList.builder().children(edgeList.getEdges()).nextIds(edgeList.getNextIds()).build();
  }
}
