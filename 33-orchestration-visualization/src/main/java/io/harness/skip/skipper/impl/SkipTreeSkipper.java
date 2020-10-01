package io.harness.skip.skipper.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.GraphVertex;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.skip.skipper.VertexSkipper;
import lombok.AllArgsConstructor;

import java.util.List;

@OwnedBy(CDC)
public class SkipTreeSkipper extends VertexSkipper {
  @Override
  public void skip(EphemeralOrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    OrchestrationAdjacencyListInternal adjacencyList = orchestrationGraph.getAdjacencyList();
    if (!adjacencyList.getGraphVertexMap().containsKey(skippedVertex.getUuid())) {
      return;
    }

    remapRelations(orchestrationGraph, skippedVertex);

    final Session session = new Session(adjacencyList);
    session.removeSubgraph(adjacencyList.getAdjacencyMap().get(skippedVertex.getUuid()).getEdges());

    removeVertex(adjacencyList, skippedVertex.getUuid());
  }

  @AllArgsConstructor
  private final class Session {
    OrchestrationAdjacencyListInternal orchestrationAdjacencyList;

    private void removeSubgraph(List<String> skippedVertexEdges) {
      if (skippedVertexEdges.isEmpty()) {
        return;
      }
      skippedVertexEdges.forEach(this ::remove);
    }

    private void remove(String vertexIdToRemove) {
      EdgeListInternal edgeList = orchestrationAdjacencyList.getAdjacencyMap().get(vertexIdToRemove);
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
