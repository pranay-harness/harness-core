package io.harness.pms.pipeline.mappers;

import io.harness.beans.EdgeList;
import io.harness.dto.GraphVertexDTO;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionGraph;
import io.harness.pms.execution.beans.ExecutionNode;
import io.harness.pms.execution.beans.ExecutionNodeAdjacencyList;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionGraphMapper {
  public final Function<GraphVertexDTO, ExecutionNode> toExecutionNode = graphVertex
      -> ExecutionNode.builder()
             .endTs(graphVertex.getEndTs())
             .failureInfo(graphVertex.getFailureInfo())
             .name(graphVertex.getName())
             .startTs(graphVertex.getStartTs())
             .identifier(graphVertex.getIdentifier())
             .status(ExecutionStatus.getExecutionStatus(graphVertex.getStatus()))
             .stepType(graphVertex.getStepType())
             .uuid(graphVertex.getUuid())
             .executableResponses(graphVertex.getExecutableResponses())
             .taskIdToProgressDataMap(graphVertex.getProgressDataMap())
             .build();

  public final Function<EdgeList, ExecutionNodeAdjacencyList> toExecutionNodeAdjacencyList = edgeList
      -> ExecutionNodeAdjacencyList.builder().children(edgeList.getEdges()).nextIds(edgeList.getNextIds()).build();

  public final Function<OrchestrationGraphDTO, ExecutionGraph> toExecutionGraph = orchestrationGraph
      -> ExecutionGraph.builder()
             .rootNodeId(orchestrationGraph.getRootNodeIds().get(0))
             .nodeMap(orchestrationGraph.getAdjacencyList().getGraphVertexMap().entrySet().stream().collect(
                 Collectors.toMap(Map.Entry::getKey, entry -> toExecutionNode.apply(entry.getValue()))))
             .nodeAdjacencyListMap(orchestrationGraph.getAdjacencyList().getAdjacencyMap().entrySet().stream().collect(
                 Collectors.toMap(Map.Entry::getKey, entry -> toExecutionNodeAdjacencyList.apply(entry.getValue()))))
             .build();

  public ExecutionGraph toExecutionGraph(OrchestrationGraphDTO orchestrationGraph) {
    return ExecutionGraph.builder()
        .rootNodeId(orchestrationGraph.getRootNodeIds().isEmpty() ? null : orchestrationGraph.getRootNodeIds().get(0))
        .nodeMap(orchestrationGraph.getAdjacencyList().getGraphVertexMap().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, entry -> toExecutionNode.apply(entry.getValue()))))
        .nodeAdjacencyListMap(orchestrationGraph.getAdjacencyList().getAdjacencyMap().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, entry -> toExecutionNodeAdjacencyList.apply(entry.getValue()))))
        .build();
  }
}
