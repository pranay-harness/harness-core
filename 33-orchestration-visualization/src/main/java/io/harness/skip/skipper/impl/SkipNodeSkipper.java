package io.harness.skip.skipper.impl;

import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraph;
import io.harness.skip.skipper.VertexSkipper;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

public class SkipNodeSkipper extends VertexSkipper {
  @Override
  public void skip(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    OrchestrationAdjacencyList adjacencyList = orchestrationGraph.getAdjacencyList();
    if (!adjacencyList.getGraphVertexMap().containsKey(skippedVertex.getUuid())) {
      return;
    }

    EdgeList skippedEdgeList = adjacencyList.getAdjacencyList().get(skippedVertex.getUuid());
    // check if we have children
    if (skippedEdgeList.getEdges().isEmpty()) {
      remapRelations(orchestrationGraph, skippedVertex);
    } else {
      promoteChildren(orchestrationGraph, skippedVertex);
    }

    removeVertex(orchestrationGraph.getAdjacencyList(), skippedVertex.getUuid());
  }

  private void promoteChildren(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    Map<String, EdgeList> adjacencyList = orchestrationGraph.getAdjacencyList().getAdjacencyList();

    EdgeList skippedVertexEdgeList = adjacencyList.get(skippedVertex.getUuid());
    if (!skippedVertexEdgeList.getPrevIds().isEmpty()) {
      skippedVertexEdgeList.getPrevIds().forEach(prevId -> {
        adjacencyList.get(prevId).getNextIds().addAll(skippedVertexEdgeList.getEdges());
        adjacencyList.get(prevId).getNextIds().remove(skippedVertex.getUuid());
      });
      skippedVertexEdgeList.getEdges().forEach(edge -> {
        EdgeList edgeList = adjacencyList.get(edge);
        edgeList.getPrevIds().addAll(skippedVertexEdgeList.getPrevIds());
        edgeList.setParentId(null);
      });
    } else if (skippedVertexEdgeList.getParentId() != null) {
      String parentId = skippedVertexEdgeList.getParentId();
      EdgeList parentEdgeList = adjacencyList.get(parentId);
      skippedVertexEdgeList.getEdges().forEach(edge -> {
        adjacencyList.get(edge).setParentId(parentId);
        parentEdgeList.getEdges().add(edge);
      });
      parentEdgeList.getEdges().remove(skippedVertex.getUuid());
    } else {
      // vertex is a rootId
      orchestrationGraph.getRootNodeIds().clear();
      orchestrationGraph.getRootNodeIds().addAll(skippedVertexEdgeList.getEdges());
      skippedVertexEdgeList.getEdges().forEach(edge -> adjacencyList.get(edge).setParentId(null));
    }

    final Session session = new Session(orchestrationGraph.getAdjacencyList());
    skippedVertexEdgeList.getNextIds().forEach(nextId -> {
      session.traverseNextIds(skippedVertexEdgeList.getEdges(), nextId, nextId);
      adjacencyList.get(nextId).getPrevIds().remove(skippedVertex.getUuid());
    });
  }

  @AllArgsConstructor
  private static final class Session {
    OrchestrationAdjacencyList orchestrationAdjacencyList;

    private void traverseNextIds(List<String> skippedVertexEdgeIds, String nextIdToSet, String skippedVertexNextId) {
      if (skippedVertexEdgeIds.isEmpty()) {
        return;
      }
      skippedVertexEdgeIds.forEach(edge -> traverse(edge, nextIdToSet, skippedVertexNextId));
    }

    private void traverse(String nextId, String nextIdToSet, String skippedVertexNextId) {
      EdgeList edgeList = orchestrationAdjacencyList.getAdjacencyList().get(nextId);
      if (edgeList == null) {
        return;
      }

      if (edgeList.getNextIds().isEmpty()) {
        edgeList.getNextIds().add(nextIdToSet);
        orchestrationAdjacencyList.getAdjacencyList().get(skippedVertexNextId).getPrevIds().add(nextId);
        return;
      }

      for (String nextToRemove : edgeList.getNextIds()) {
        traverse(nextToRemove, nextIdToSet, skippedVertexNextId);
      }
    }
  }
}
