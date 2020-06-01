package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.resource.GraphVertex;
import io.harness.resource.Subgraph;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Redesign
@OwnedBy(CDC)
@Singleton
public class GraphGenerator {
  @Inject private NodeExecutionService nodeExecutionService;

  public GraphVertex generateGraphVertex(String planExecutionId) {
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutions(planExecutionId);
    if (isEmpty(nodeExecutions)) {
      throw new InvalidRequestException("No nodes found for planExecutionId [" + planExecutionId + "]");
    }

    return generate(nodeExecutions);
  }

  GraphVertex generate(List<NodeExecution> nodeExecutions) {
    Map<String, NodeExecution> nodeExIdMap = obtainNodeExecutionMap(nodeExecutions);
    Map<String, List<String>> parentIdMap = obtainParentIdMap(nodeExecutions);

    final GraphGeneratorSession session = new GraphGeneratorSession(nodeExIdMap, parentIdMap);
    return session.generateGraph(obtainStartingNodeExId(nodeExecutions));
  }

  private String obtainStartingNodeExId(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> isEmpty(node.getParentId()) && isEmpty(node.getPreviousId()))
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("Starting node is not found"))
        .getUuid();
  }

  private Map<String, NodeExecution> obtainNodeExecutionMap(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream().collect(toMap(NodeExecution::getUuid, identity()));
  }

  private Map<String, List<String>> obtainParentIdMap(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> isNotEmpty(node.getParentId()) && isEmpty(node.getPreviousId()))
        .collect(groupingBy(NodeExecution::getParentId, mapping(NodeExecution::getUuid, toList())));
  }

  private static class GraphGeneratorSession {
    private final Map<String, NodeExecution> nodeExIdMap;
    private final Map<String, List<String>> parentIdMap;

    GraphGeneratorSession(Map<String, NodeExecution> nodeExIdMap, Map<String, List<String>> parentIdMap) {
      this.nodeExIdMap = nodeExIdMap;
      this.parentIdMap = parentIdMap;
    }

    private GraphVertex generateGraph(String nodeExId) {
      NodeExecution currentNode = nodeExIdMap.get(nodeExId);
      if (currentNode == null) {
        throw new UnexpectedException("The node with id [" + nodeExId + "] is not found");
      }

      GraphVertex graphVertex = GraphVertex.builder()
                                    .uuid(currentNode.getUuid())
                                    .name(currentNode.getNode().getName())
                                    .startTs(currentNode.getStartTs())
                                    .endTs(currentNode.getEndTs())
                                    .stepType(currentNode.getNode().getStepType().getType())
                                    .status(currentNode.getStatus())
                                    .build();

      if (parentIdMap.containsKey(currentNode.getUuid())) {
        graphVertex.setSubgraph(new Subgraph(currentNode.getMode()));
        for (String nextNodeExId : parentIdMap.get(currentNode.getUuid())) {
          GraphVertex subgraph = generateGraph(nextNodeExId);
          graphVertex.getSubgraph().getVertices().add(subgraph);
        }
      }

      if (currentNode.getNextId() != null) {
        GraphVertex nextGraphVertex = generateGraph(currentNode.getNextId());
        graphVertex.setNext(nextGraphVertex);
      }

      return graphVertex;
    }
  }
}