package io.harness.generator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.ExecutionModeUtils.isChainMode;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.sdk.core.data.Outcome;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Redesign
@OwnedBy(HarnessTeam.CDC)
@Singleton
public class OrchestrationAdjacencyListGenerator {
  @Inject private OutcomeService outcomeService;

  public OrchestrationAdjacencyListInternal generateAdjacencyList(
      String startingNodeExId, List<NodeExecution> nodeExecutions, boolean isOutcomePresent) {
    if (isEmpty(startingNodeExId)) {
      log.warn("Starting node cannot be null");
      return null;
    }
    return generateList(startingNodeExId, nodeExecutions, isOutcomePresent);
  }

  public void populateAdjacencyList(
      OrchestrationAdjacencyListInternal adjacencyListInternal, List<NodeExecution> nodeExecutions) {
    nodeExecutions.sort(Comparator.comparing(NodeExecution::getCreatedAt));

    for (NodeExecution nodeExecution : nodeExecutions) {
      populateAdjacencyList(adjacencyListInternal, nodeExecution);
    }
  }

  public void populateAdjacencyList(
      OrchestrationAdjacencyListInternal adjacencyListInternal, NodeExecution nodeExecution) {
    Map<String, GraphVertex> graphVertexMap = adjacencyListInternal.getGraphVertexMap();
    Map<String, EdgeListInternal> adjacencyList = adjacencyListInternal.getAdjacencyMap();

    String currentUuid = nodeExecution.getUuid();
    graphVertexMap.put(currentUuid, GraphVertexConverter.convertFrom(nodeExecution));

    // compute adjList
    String parentId = null;
    List<String> prevIds = new ArrayList<>();
    if (isIdPresent(nodeExecution.getPreviousId())) {
      adjacencyList.get(nodeExecution.getPreviousId()).getNextIds().add(currentUuid);
      prevIds.add(nodeExecution.getPreviousId());
    } else if (isIdPresent(nodeExecution.getParentId())) {
      parentId = nodeExecution.getParentId();
      EdgeListInternal parentEdgeList = adjacencyList.get(parentId);
      if (isChainNonInitialVertex(graphVertexMap.get(parentId).getMode(), parentEdgeList)) {
        appendToChainEnd(adjacencyList, parentEdgeList.getEdges().get(0), currentUuid, prevIds);
      } else {
        parentEdgeList.getEdges().add(currentUuid);
      }
    }
    adjacencyList.put(currentUuid,
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(prevIds)
            .parentId(parentId)
            .build());
  }

  public OrchestrationAdjacencyListInternal generatePartialAdjacencyList(
      String startingId, OrchestrationAdjacencyListInternal cachedAdjacencyList) {
    OrchestrationAdjacencyListInternal adjacencyListInternal = OrchestrationAdjacencyListInternal.builder()
                                                                   .graphVertexMap(new HashMap<>())
                                                                   .adjacencyMap(new HashMap<>())
                                                                   .build();
    populateAdjacencyListWithEdgesStartingFrom(startingId, cachedAdjacencyList, adjacencyListInternal);

    return adjacencyListInternal;
  }

  boolean isIdPresent(String id) {
    return EmptyPredicate.isNotEmpty(id);
  }

  boolean isChainNonInitialVertex(ExecutionMode mode, EdgeListInternal parentEdgeList) {
    return isChainMode(mode) && !parentEdgeList.getEdges().isEmpty();
  }

  OrchestrationAdjacencyListInternal generateList(
      String startingNodeExId, List<NodeExecution> nodeExecutions, boolean isOutcomePresent) {
    final GraphGeneratorSession session = createSession(nodeExecutions);
    return session.generateListStartingFrom(startingNodeExId, isOutcomePresent);
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

  private void appendToChainEnd(
      Map<String, EdgeListInternal> adjacencyList, String firstChainId, String nextId, List<String> prevIds) {
    EdgeListInternal edgeList = adjacencyList.get(firstChainId);
    String nextEdgeId = firstChainId;
    while (!edgeList.getNextIds().isEmpty()) {
      nextEdgeId = edgeList.getNextIds().get(0);
      edgeList = adjacencyList.get(nextEdgeId);
    }

    prevIds.add(nextEdgeId);
    edgeList.getNextIds().add(nextId);
  }

  private void populateAdjacencyListWithEdgesStartingFrom(String startingId,
      OrchestrationAdjacencyListInternal listToTraverse, OrchestrationAdjacencyListInternal listToPopulate) {
    EdgeListInternal edgeListInternal = listToTraverse.getAdjacencyMap().get(startingId);
    if (edgeListInternal == null) {
      return;
    }

    for (String edgeId : edgeListInternal.getEdges()) {
      populateAdjacencyListFor(edgeId, listToTraverse, listToPopulate);
    }

    listToPopulate.getGraphVertexMap().put(startingId, listToTraverse.getGraphVertexMap().get(startingId));
    listToPopulate.getAdjacencyMap().put(startingId, listToTraverse.getAdjacencyMap().get(startingId));
  }

  private void populateAdjacencyListFor(String currentId, OrchestrationAdjacencyListInternal listToTraverse,
      OrchestrationAdjacencyListInternal listToPopulate) {
    EdgeListInternal edgeListInternal = listToTraverse.getAdjacencyMap().get(currentId);
    if (edgeListInternal == null) {
      return;
    }

    for (String edgeId : edgeListInternal.getEdges()) {
      populateAdjacencyListFor(edgeId, listToTraverse, listToPopulate);
    }

    for (String nextId : edgeListInternal.getNextIds()) {
      populateAdjacencyListFor(nextId, listToTraverse, listToPopulate);
    }

    listToPopulate.getGraphVertexMap().put(currentId, listToTraverse.getGraphVertexMap().get(currentId));
    listToPopulate.getAdjacencyMap().put(currentId, listToTraverse.getAdjacencyMap().get(currentId));
  }

  private class GraphGeneratorSession {
    private final Map<String, NodeExecution> nodeExIdMap;
    private final Map<String, List<String>> parentIdMap;

    GraphGeneratorSession(Map<String, NodeExecution> nodeExIdMap, Map<String, List<String>> parentIdMap) {
      this.nodeExIdMap = nodeExIdMap;
      this.parentIdMap = parentIdMap;
    }

    private OrchestrationAdjacencyListInternal generateListStartingFrom(
        String startingNodeId, boolean isOutcomePresent) {
      if (startingNodeId == null) {
        throw new InvalidRequestException("The starting node id cannot be null");
      }

      Map<String, String> chainMap = new HashMap<>();
      Map<String, GraphVertex> graphVertexMap = new HashMap<>();
      Map<String, EdgeListInternal> adjacencyList = new HashMap<>();

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
        List<String> nextIds = new ArrayList<>();

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
          nextIds.add(nextNodeId);
        } else if (chainMap.containsKey(currentNodeId)) {
          nextNodeId = chainMap.get(currentNodeId);
          queue.add(nextNodeId);
          nextIds.add(nextNodeId);
        }

        adjacencyList.put(currentNodeId, EdgeListInternal.builder().edges(edges).nextIds(nextIds).build());
      }

      return OrchestrationAdjacencyListInternal.builder()
          .graphVertexMap(graphVertexMap)
          .adjacencyMap(adjacencyList)
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
