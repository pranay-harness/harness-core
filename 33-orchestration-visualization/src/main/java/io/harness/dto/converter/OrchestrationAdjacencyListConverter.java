package io.harness.dto.converter;

import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationAdjacencyListInternal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationAdjacencyListConverter {
  public OrchestrationAdjacencyList convertFrom(OrchestrationAdjacencyListInternal adjacencyListInternal) {
    return OrchestrationAdjacencyList.builder()
        .graphVertexMap(adjacencyListInternal.getGraphVertexMap())
        .adjacencyList(adjacencyListInternal.getAdjacencyList())
        .build();
  }
}
