/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.skip.service.impl;

import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.GraphVertex;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.skip.factory.VertexSkipperFactory;
import io.harness.skip.service.VertexSkipperService;
import io.harness.skip.skipper.VertexSkipper;

import com.google.inject.Inject;
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
