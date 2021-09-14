/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.converter.EdgeListConverter;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.dto.OrchestrationAdjacencyListDTO;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

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
