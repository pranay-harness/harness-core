package io.harness.generator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.facilitator.modes.ExecutionMode.isChainMode;
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
import io.harness.beans.OrchestrationAdjacencyListInternal;
import io.harness.beans.Subgraph;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.data.Outcome;
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

  public OrchestrationAdjacencyListInternal generateAdjacencyList(
      String startingNodeExId, List<NodeExecution> nodeExecutions, boolean isOutcomePresent) {
    if (isEmpty(startingNodeExId)) {
      logger.warn("Starting node cannot be null");
      return null;
    }
    return generateList(startingNodeExId, nodeExecutions, isOutcomePresent);
  }

  public void populateAdjacencyList(
      OrchestrationAdjacencyListInternal adjacencyListInternal, List<NodeExecution> nodeExecutions) {
    nodeExecutions.sort(Comparator.comparing(NodeExecution::getCreatedAt));

    Map<String, GraphVertex> graphVertexMap = adjacencyListInternal.getGraphVertexMap();
    Map<String, EdgeList> adjacencyList = adjacencyListInternal.getAdjacencyList();

    for (NodeExecution nodeExecution : nodeExecutions) {
      String currentUuid = nodeExecution.getUuid();
      graphVertexMap.put(currentUuid, GraphVertexConverter.convertFrom(nodeExecution));

      // compute adjList
      if (isPreviousIdPresent(nodeExecution.getPreviousId())) {
        adjacencyList.get(nodeExecution.getPreviousId()).setNext(currentUuid);
      } else if (isParentIdPresent(nodeExecution.getParentId())) {
        String parentId = nodeExecution.getParentId();
        EdgeList parentEdgeList = adjacencyList.get(parentId);
        if (isChainNonInitialVertex(graphVertexMap.get(parentId).getMode(), parentEdgeList)) {
          appendToChainEnd(adjacencyList, parentEdgeList.getEdges().get(0), currentUuid);
        } else {
          parentEdgeList.getEdges().add(currentUuid);
        }
      }
      adjacencyList.put(
          currentUuid, EdgeList.builder().edges(new ArrayList<>()).next(nodeExecution.getNextId()).build());
    }
  }

  boolean isPreviousIdPresent(String previousId) {
    return EmptyPredicate.isNotEmpty(previousId);
  }

  boolean isParentIdPresent(String parentId) {
    return EmptyPredicate.isNotEmpty(parentId);
  }

  boolean isChainNonInitialVertex(ExecutionMode mode, EdgeList parentEdgeList) {
    return isChainMode(mode) && !parentEdgeList.getEdges().isEmpty();
  }

  OrchestrationAdjacencyListInternal generateList(
      String startingNodeExId, List<NodeExecution> nodeExecutions, boolean isOutcomePresent) {
    final GraphGeneratorSession session = createSession(nodeExecutions);
    return session.generateListStartingFrom(startingNodeExId, isOutcomePresent);
  }

  GraphVertex generate(String startingNodeExId, List<NodeExecution> nodeExecutions) {
    final GraphGeneratorSession session = createSession(nodeExecutions);
    return session.generateGraph(startingNodeExId);
  }

  private GraphGeneratorSession createSession(List<NodeExecution> nodeExecutions) {
    Map<String, NodeExecution> nodeExIdMap = obtainNodeExecutionMap(nodeExecutions);
    Map<String, List<String>> parentIdMap = obtainParentIdMap(nodeExecutions);

    return new GraphGeneratorSession(nodeExIdMap, parentIdMap);
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

  private void appendToChainEnd(Map<String, EdgeList> adjacencyList, String firstChainId, String nextId) {
    EdgeList edgeList = adjacencyList.get(firstChainId);
    while (edgeList.getNext() != null) {
      edgeList = adjacencyList.get(edgeList.getNext());
    }

    edgeList.setNext(nextId);
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

    private OrchestrationAdjacencyListInternal generateListStartingFrom(
        String startingNodeId, boolean isOutcomePresent) {
      if (startingNodeId == null) {
        throw new InvalidRequestException("The starting node id cannot be null");
      }

      Map<String, String> chainMap = new HashMap<>();
      Map<String, GraphVertex> graphVertexMap = new HashMap<>();
      Map<String, EdgeList> adjacencyList = new HashMap<>();

      LinkedList<String> queue = new LinkedList<>();
      queue.add(startingNodeId);

      while (!queue.isEmpty()) {
        String currentNodeId = queue.removeFirst();
        NodeExecution nodeExecution = nodeExIdMap.get(currentNodeId);

        List<Outcome> outcomes = new ArrayList<>();
        if (isOutcomePresent) {
          outcomes = outcomeService.findAllByRuntimeId(nodeExecution.getAmbiance().getPlanExecutionId(), currentNodeId);
        }

        GraphVertex graphVertex = GraphVertexConverter.convertFrom(nodeExecution, outcomes);

        if (graphVertexMap.containsKey(graphVertex.getUuid())) {
          continue;
        }

        graphVertexMap.put(graphVertex.getUuid(), graphVertex);

        List<String> edges = new ArrayList<>();

        if (parentIdMap.containsKey(currentNodeId) && !parentIdMap.get(currentNodeId).isEmpty()) {
          List<String> childNodeIds = parentIdMap.get(currentNodeId);

          if (isChainMode(graphVertex.getMode())) {
            String chainStartingId = populateChainMap(chainMap, childNodeIds);
            edges.add(chainStartingId);
            queue.add(chainStartingId);
          } else {
            edges.addAll(childNodeIds);
            queue.addAll(childNodeIds);
          }
        }

        String nextNodeId = nodeExecution.getNextId();
        if (EmptyPredicate.isNotEmpty(nextNodeId)) {
          if (chainMap.containsKey(currentNodeId)) {
            chainMap.put(nextNodeId, chainMap.get(currentNodeId));
            chainMap.remove(currentNodeId);
          }
          queue.add(nextNodeId);
        } else if (chainMap.containsKey(currentNodeId)) {
          nextNodeId = chainMap.get(currentNodeId);
          queue.add(nextNodeId);
        }

        adjacencyList.put(currentNodeId, EdgeList.builder().edges(edges).next(nextNodeId).build());
      }

      return OrchestrationAdjacencyListInternal.builder()
          .graphVertexMap(graphVertexMap)
          .adjacencyList(adjacencyList)
          .build();
    }

    /**
     * Population of chainMap with chainIds except for the last id <br>
     * Ex.: pin1 -> pin2 -> pin3 <br>
     *      map = {pin1, pin2}, {pin2, pin3} <br>
     * <br>
     * @param chainMap map containing chain order
     * @param chainIds list which contains ids of the chain
     * @return starting point of a chain
     */
    private String populateChainMap(Map<String, String> chainMap, List<String> chainIds) {
      for (int i = 1; i < chainIds.size(); ++i) {
        chainMap.put(chainIds.get(i - 1), chainIds.get(i));
      }
      return chainIds.get(0);
    }
  }
}