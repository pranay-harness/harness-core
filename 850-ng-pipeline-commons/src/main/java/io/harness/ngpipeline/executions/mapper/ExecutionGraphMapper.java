package io.harness.ngpipeline.executions.mapper;

import io.harness.beans.EdgeList;
import io.harness.dto.GraphVertexDTO;
import io.harness.dto.OrchestrationAdjacencyListDTO;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionGraph;
import io.harness.pms.execution.beans.ExecutionGraph.ExecutionGraphBuilder;
import io.harness.pms.execution.beans.ExecutionNode;
import io.harness.pms.execution.beans.ExecutionNodeAdjacencyList;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

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
        .name(graphVertex.getName())
        .startTs(graphVertex.getStartTs())
        .status(ExecutionStatus.getExecutionStatus(graphVertex.getStatus()))
        .stepType(graphVertex.getStepType())
        .uuid(graphVertex.getUuid())
        .executableResponsesMetadata(graphVertex.getExecutableResponsesMetadata())
        .taskIdToProgressDataMap(graphVertex.getProgressDataMap())
        .build();
  }

  @NonNull
  static ExecutionNodeAdjacencyList toExecutionNodeAdjacencyList(@NonNull EdgeList edgeList) {
    return ExecutionNodeAdjacencyList.builder().children(edgeList.getEdges()).nextIds(edgeList.getNextIds()).build();
  }
}
