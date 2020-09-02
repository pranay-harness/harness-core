package io.harness.generator;

import static io.harness.beans.EdgeList.Edge.EdgeType.CHILD;
import static io.harness.beans.EdgeList.Edge.EdgeType.SIBLING;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.facilitator.modes.ExecutionMode.CHILDREN;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EdgeList;
import io.harness.beans.EdgeList.Edge;
import io.harness.beans.EdgeList.EdgeListBuilder;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.Subgraph;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutionMode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Redesign
@OwnedBy(HarnessTeam.CDC)
@Singleton
public class GraphGenerator {
  @Inject private OutcomeService outcomeService;

  public GraphVertex generateGraphVertexStartingFrom(String startingNodeExId, List<NodeExecution> nodeExecutions) {
    if (EmptyPredicate.isEmpty(startingNodeExId)) {
      logger.warn("Starting node cannot be null");
      return null;
    }
    return generate(startingNodeExId, nodeExecutions);
  }

  public OrchestrationAdjacencyList generateAdjacencyList(String startingNodeExId, List<NodeExecution> nodeExecutions) {
    if (isEmpty(startingNodeExId)) {
      logger.warn("Starting node cannot be null");
      return null;
    }
    return generateList(startingNodeExId, nodeExecutions);
  }

  OrchestrationAdjacencyList generateList(String startingNodeExId, List<NodeExecution> nodeExecutions) {
    Map<String, NodeExecution> nodeExIdMap = obtainNodeExecutionMap(nodeExecutions);
    Map<String, List<String>> parentIdMap = obtainParentIdMap(nodeExecutions);

    final GraphGeneratorSession session = new GraphGeneratorSession(nodeExIdMap, parentIdMap);
    return session.generateList();
  }

  GraphVertex generate(String startingNodeExId, List<NodeExecution> nodeExecutions) {
    Map<String, NodeExecution> nodeExIdMap = obtainNodeExecutionMap(nodeExecutions);
    Map<String, List<String>> parentIdMap = obtainParentIdMap(nodeExecutions);

    final GraphGeneratorSession session = new GraphGeneratorSession(nodeExIdMap, parentIdMap);
    return session.generateGraph(startingNodeExId);
  }

  private Map<String, NodeExecution> obtainNodeExecutionMap(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream().collect(toMap(NodeExecution::getUuid, identity()));
  }

  private Map<String, List<String>> obtainParentIdMap(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> EmptyPredicate.isNotEmpty(node.getParentId()) && EmptyPredicate.isEmpty(node.getPreviousId()))
        .sorted(Comparator.comparingLong(NodeExecution::getCreatedAt))
        .collect(groupingBy(NodeExecution::getParentId, mapping(NodeExecution::getUuid, toList())));
  }

  private class GraphGeneratorSession {
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
                                    .initialWaitDuration(currentNode.getInitialWaitDuration())
                                    .lastUpdatedAt(currentNode.getLastUpdatedAt())
                                    .stepType(currentNode.getNode().getStepType().getType())
                                    .status(currentNode.getStatus())
                                    .failureInfo(currentNode.getFailureInfo())
                                    .stepParameters(currentNode.getResolvedStepParameters())
                                    .interruptHistories(currentNode.getInterruptHistories())
                                    .outcomes(outcomeService.findAllByRuntimeId(
                                        currentNode.getAmbiance().getPlanExecutionId(), currentNode.getUuid()))
                                    .retryIds(currentNode.getRetryIds())
                                    .build();

      if (parentIdMap.containsKey(currentNode.getUuid())) {
        graphVertex.setSubgraph(new Subgraph(currentNode.getMode()));
        if (currentNode.getMode() == ExecutionMode.CHILD_CHAIN) {
          GraphVertex subgraph = new GraphVertex();
          for (String nextChainNodeId : parentIdMap.get(currentNode.getUuid())) {
            generateChain(subgraph, nextChainNodeId);
          }
          graphVertex.getSubgraph().getVertices().add(subgraph.getNext());
        } else {
          for (String nextNodeExId : parentIdMap.get(currentNode.getUuid())) {
            GraphVertex subgraph = generateGraph(nextNodeExId);
            graphVertex.getSubgraph().getVertices().add(subgraph);
          }
        }
      }

      if (currentNode.getNextId() != null) {
        GraphVertex nextGraphVertex = generateGraph(currentNode.getNextId());
        graphVertex.setNext(nextGraphVertex);
      }

      return graphVertex;
    }

    private void generateChain(GraphVertex vertex, String nextChainNodeId) {
      GraphVertex currentVertex = vertex;
      while (currentVertex.getNext() != null) {
        currentVertex = currentVertex.getNext();
      }

      currentVertex.setNext(generateGraph(nextChainNodeId));
    }

    private OrchestrationAdjacencyList generateList() {
      Map<String, GraphVertex> graphVertexMap = nodeExIdMap.entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> convertToGraphVertex(e.getValue())));
      Map<String, EdgeList> adjacencyList = nodeExIdMap.entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> convertToEdgeList(e.getKey())));

      return OrchestrationAdjacencyList.builder().graphVertexMap(graphVertexMap).adjacencyList(adjacencyList).build();
    }

    private GraphVertex convertToGraphVertex(NodeExecution nodeExecution) {
      return GraphVertex.builder()
          .uuid(nodeExecution.getUuid())
          .name(nodeExecution.getNode().getName())
          .startTs(nodeExecution.getStartTs())
          .endTs(nodeExecution.getEndTs())
          .initialWaitDuration(nodeExecution.getInitialWaitDuration())
          .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
          .stepType(nodeExecution.getNode().getStepType().getType())
          .status(nodeExecution.getStatus())
          .failureInfo(nodeExecution.getFailureInfo())
          .stepParameters(nodeExecution.getResolvedStepParameters())
          .interruptHistories(nodeExecution.getInterruptHistories())
          .outcomes(outcomeService.findAllByRuntimeId(
              nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid()))
          .retryIds(nodeExecution.getRetryIds())
          .build();
    }

    private EdgeList convertToEdgeList(String nodeExecutionId) {
      EdgeListBuilder builder = EdgeList.builder();

      List<Edge> edges = new ArrayList<>();
      Map<String, List<Edge>> groupedEdges = new HashMap<>();

      if (parentIdMap.containsKey(nodeExecutionId)) {
        if (CHILDREN == nodeExIdMap.get(nodeExecutionId).getMode()) {
          groupedEdges.put("PARALLEL", convertToEdge(parentIdMap.get(nodeExecutionId)));
        } else {
          edges.addAll(convertToEdge(parentIdMap.get(nodeExecutionId)));
        }
      }

      if (nodeExIdMap.get(nodeExecutionId).getNextId() != null) {
        edges.add(Edge.builder().toNodeId(nodeExIdMap.get(nodeExecutionId).getNextId()).edgeType(SIBLING).build());
      }

      return builder.groupedEdges(groupedEdges).edges(edges).build();
    }

    private List<Edge> convertToEdge(List<String> nodeExecutionIds) {
      return nodeExecutionIds.stream()
          .map(id -> Edge.builder().toNodeId(id).edgeType(CHILD).build())
          .collect(Collectors.toList());
    }
  }
}