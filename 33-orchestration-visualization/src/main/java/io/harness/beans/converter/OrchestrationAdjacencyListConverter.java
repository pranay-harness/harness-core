package io.harness.beans.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationAdjacencyListConverter {
  public OrchestrationAdjacencyList convertFrom(OrchestrationAdjacencyListInternal adjacencyListInternal) {
    return OrchestrationAdjacencyList.builder()
        .graphVertexMap(adjacencyListInternal.getGraphVertexMap())
        .adjacencyMap(adjacencyListInternal.getAdjacencyMap().entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, edgeListInternal -> EdgeListConverter.convertFrom(edgeListInternal.getValue()))))
        .build();
  }
}
