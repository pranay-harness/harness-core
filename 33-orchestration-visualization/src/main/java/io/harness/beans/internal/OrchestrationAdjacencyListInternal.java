package io.harness.beans.internal;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
public class OrchestrationAdjacencyListInternal {
  Map<String, GraphVertex> graphVertexMap;
  Map<String, EdgeListInternal> adjacencyMap;
}
