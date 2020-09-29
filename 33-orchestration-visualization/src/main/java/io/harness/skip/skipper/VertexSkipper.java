package io.harness.skip.skipper;

import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraph;

import java.util.List;
import java.util.Map;

public abstract class VertexSkipper {
  public void remapRelations(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    OrchestrationAdjacencyList adjacencyList = orchestrationGraph.getAdjacencyList();
    EdgeList skippedVertexEdgeList = adjacencyList.getAdjacencyList().get(skippedVertex.getUuid());

    // remapping relations
    if (!skippedVertexEdgeList.getPrevIds().isEmpty()) {
      for (String prevVertexId : skippedVertexEdgeList.getPrevIds()) {
        remapRelations(adjacencyList, skippedVertex.getUuid(), prevVertexId);
      }
    } else if (skippedVertexEdgeList.getParentId() != null) {
      remapRelationsForParent(adjacencyList, skippedVertex.getUuid(), skippedVertexEdgeList.getParentId());
    } else {
      // skipped vertex is a rootId
      remapRelations(adjacencyList, skippedVertex.getUuid(), null);
      orchestrationGraph.getRootNodeIds().clear();
      orchestrationGraph.getRootNodeIds().addAll(skippedVertexEdgeList.getNextIds());
    }
  }

  public void removeVertex(OrchestrationAdjacencyList orchestrationAdjacencyList, String skippedVertexId) {
    orchestrationAdjacencyList.getGraphVertexMap().remove(skippedVertexId);
    orchestrationAdjacencyList.getAdjacencyList().remove(skippedVertexId);
  }

  public abstract void skip(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex);

  private void remapRelations(
      OrchestrationAdjacencyList orchestrationAdjacencyList, String skippedVertexId, String precedingId) {
    Map<String, EdgeList> adjacencyMap = orchestrationAdjacencyList.getAdjacencyList();
    List<String> skippedVertexNextIds = getNextIdsFor(adjacencyMap, skippedVertexId);

    if (precedingId != null && adjacencyMap.containsKey(precedingId)) {
      List<String> precedingVertexNextIds = getNextIdsFor(adjacencyMap, precedingId);
      precedingVertexNextIds.remove(skippedVertexId);
      precedingVertexNextIds.addAll(skippedVertexNextIds);
    }

    skippedVertexNextIds.forEach(nextId -> {
      List<String> prevIds = getPrevIdsFor(adjacencyMap, nextId);
      prevIds.remove(skippedVertexId);
      if (precedingId != null) {
        prevIds.add(precedingId);
      }
    });
  }

  private void remapRelationsForParent(
      OrchestrationAdjacencyList orchestrationAdjacencyList, String skippedVertexId, String parentId) {
    Map<String, EdgeList> adjacencyList = orchestrationAdjacencyList.getAdjacencyList();
    List<String> skippedVertexNextIds = getNextIdsFor(adjacencyList, skippedVertexId);

    if (adjacencyList.containsKey(parentId)) {
      List<String> parentEdges = adjacencyList.get(parentId).getEdges();
      parentEdges.remove(skippedVertexId);
      parentEdges.addAll(skippedVertexNextIds);
    }

    skippedVertexNextIds.forEach(nextId -> {
      EdgeList edgeList = adjacencyList.get(nextId);
      edgeList.setParentId(parentId);
      edgeList.getPrevIds().remove(skippedVertexId);
    });
  }

  private List<String> getPrevIdsFor(Map<String, EdgeList> adjacencyList, String vertexId) {
    return adjacencyList.get(vertexId).getPrevIds();
  }

  private List<String> getNextIdsFor(Map<String, EdgeList> adjacencyList, String vertexId) {
    return adjacencyList.get(vertexId).getNextIds();
  }
}
