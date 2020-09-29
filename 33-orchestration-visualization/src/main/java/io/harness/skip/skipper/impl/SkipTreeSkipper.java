package io.harness.skip.skipper.impl;

import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraph;
import io.harness.skip.skipper.VertexSkipper;
import lombok.AllArgsConstructor;

import java.util.List;

public class SkipTreeSkipper extends VertexSkipper {
  @Override
  public void skip(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    OrchestrationAdjacencyList adjacencyList = orchestrationGraph.getAdjacencyList();
    if (!adjacencyList.getGraphVertexMap().containsKey(skippedVertex.getUuid())) {
      return;
    }

    remapRelations(orchestrationGraph, skippedVertex);

    final Session session = new Session(adjacencyList);
    session.removeSubgraph(adjacencyList.getAdjacencyList().get(skippedVertex.getUuid()).getEdges());

    removeVertex(adjacencyList, skippedVertex.getUuid());
  }

  @AllArgsConstructor
  private final class Session {
    OrchestrationAdjacencyList orchestrationAdjacencyList;

    private void removeSubgraph(List<String> skippedVertexEdges) {
      if (skippedVertexEdges.isEmpty()) {
        return;
      }
      skippedVertexEdges.forEach(this ::remove);
    }

    private void remove(String vertexIdToRemove) {
      EdgeList edgeList = orchestrationAdjacencyList.getAdjacencyList().get(vertexIdToRemove);
      if (edgeList == null) {
        return;
      }

      for (String childToRemove : edgeList.getEdges()) {
        remove(childToRemove);
      }

      for (String nextToRemove : edgeList.getNextIds()) {
        remove(nextToRemove);
      }

      removeVertex(orchestrationAdjacencyList, vertexIdToRemove);
    }
  }
}
