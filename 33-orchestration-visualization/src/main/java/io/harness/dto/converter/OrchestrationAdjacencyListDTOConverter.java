package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.converter.EdgeListConverter;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.dto.OrchestrationAdjacencyListDTO;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationAdjacencyListDTOConverter {
  public OrchestrationAdjacencyListDTO convertFrom(OrchestrationAdjacencyListInternal adjacencyListInternal) {
    return OrchestrationAdjacencyListDTO.builder()
        .graphVertexMap(adjacencyListInternal.getGraphVertexMap().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, m -> GraphVertexDTOConverter.toGraphVertexDTO.apply(m.getValue()))))
        .adjacencyMap(adjacencyListInternal.getAdjacencyMap().entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, edgeListInternal -> EdgeListConverter.convertFrom(edgeListInternal.getValue()))))
        .build();
  }
}
