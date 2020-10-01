package io.harness.skip.service.impl;

import com.google.inject.Inject;

import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.GraphVertex;
import io.harness.skip.SkipType;
import io.harness.skip.factory.VertexSkipperFactory;
import io.harness.skip.service.VertexSkipperService;
import io.harness.skip.skipper.VertexSkipper;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class VertexSkipperServiceImpl implements VertexSkipperService {
  @Inject VertexSkipperFactory vertexSkipperFactory;

  @Override
  public void removeSkippedVertices(EphemeralOrchestrationGraph orchestrationGraph) {
    Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();

    graphVertexMap.values()
        .stream()
        .filter(vertex -> vertex.getSkipType() != SkipType.NOOP)
        .collect(Collectors.toList())
        .stream()
        .sorted(Comparator.comparing(GraphVertex::getLastUpdatedAt).reversed())
        .forEach(skippedVertex -> {
          VertexSkipper vertexSkipper = vertexSkipperFactory.obtainVertexSkipper(skippedVertex.getSkipType());
          vertexSkipper.skip(orchestrationGraph, skippedVertex);
        });
  }
}
