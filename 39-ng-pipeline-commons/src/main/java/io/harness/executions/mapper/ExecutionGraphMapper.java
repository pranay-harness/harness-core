package io.harness.executions.mapper;

import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraph;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.beans.ExecutionGraph.ExecutionGraphBuilder;
import io.harness.executions.beans.ExecutionNode;
import io.harness.executions.beans.ExecutionNodeAdjacencyList;
import io.harness.executions.beans.PipelineExecutionStatus;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ExecutionGraphMapper {
  private ExecutionGraphMapper() {}

  @NonNull
  public static ExecutionGraph toExecutionGraph(@NonNull OrchestrationGraph orchestrationGraph) {
    ExecutionGraphBuilder executionGraphBuilder =
        ExecutionGraph.builder().rootNodeId(orchestrationGraph.getRootNodeIds().get(0));

    OrchestrationAdjacencyList adjacencyList = orchestrationGraph.getAdjacencyList();

    Map<String, ExecutionNode> nodeMap = new HashMap<>();
    adjacencyList.getGraphVertexMap().forEach((key, value) -> nodeMap.put(key, toExecutionNode(value)));

    Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap = new HashMap<>();
    adjacencyList.getAdjacencyList().forEach(
        (key, value) -> nodeAdjacencyListMap.put(key, toExecutionNodeAdjacencyList(value)));

    return executionGraphBuilder.nodeMap(nodeMap).nodeAdjacencyListMap(nodeAdjacencyListMap).build();
  }

  @NonNull
  static ExecutionNode toExecutionNode(@NonNull GraphVertex graphVertex) {
    return ExecutionNode.builder()
        .endTs(graphVertex.getEndTs())
        .failureInfo(graphVertex.getFailureInfo())
        .name(graphVertex.getName())
        .startTs(graphVertex.getStartTs())
        .status(PipelineExecutionStatus.SUCCESS) // TODO: use status mapper
        .stepType(graphVertex.getStepType())
        .uuid(graphVertex.getUuid())
        .build();
  }

  @NonNull
  static ExecutionNodeAdjacencyList toExecutionNodeAdjacencyList(@NonNull EdgeList edgeList) {
    return ExecutionNodeAdjacencyList.builder()
        .children(edgeList.getEdges())
        .next(edgeList.getNextIds().get(0))
        .build();
  }
}
