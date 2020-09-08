package io.harness.generator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.Subgraph;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutionMode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    return session.generateListStartingFrom(startingNodeExId);
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

    private OrchestrationAdjacencyList generateListStartingFrom(String startingNodeId) {
      if (startingNodeId == null) {
        throw new InvalidRequestException("The starting node id cannot be null");
      }

      Map<String, GraphVertex> graphVertexMap = new HashMap<>();
      Map<String, EdgeList> adjacencyList = new HashMap<>();

      LinkedList<String> queue = new LinkedList<>();
      queue.add(startingNodeId);

      while (!queue.isEmpty()) {
        String currentNodeId = queue.removeFirst();
        GraphVertex graphVertex = convertToGraphVertex(nodeExIdMap.get(currentNodeId));

        if (graphVertexMap.containsKey(graphVertex.getUuid())) {
          continue;
        }

        graphVertexMap.put(graphVertex.getUuid(), graphVertex);

        List<String> edges = new ArrayList<>();

        if (parentIdMap.containsKey(currentNodeId)) {
          List<String> childNodeIds = parentIdMap.get(currentNodeId);
          edges.addAll(childNodeIds);
          queue.addAll(childNodeIds);
        }

        String nextNodeId = nodeExIdMap.get(currentNodeId).getNextId();
        if (EmptyPredicate.isNotEmpty(nextNodeId)) {
          queue.add(nextNodeId);
        }

        adjacencyList.put(currentNodeId, EdgeList.builder().edges(edges).next(nextNodeId).build());
      }

      return OrchestrationAdjacencyList.builder().graphVertexMap(graphVertexMap).adjacencyList(adjacencyList).build();
    }

    private GraphVertex convertToGraphVertex(NodeExecution nodeExecution) {
      return GraphVertex.builder()
          .uuid(nodeExecution.getUuid())
          .planNodeId(nodeExecution.getNode().getUuid())
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
  }
}