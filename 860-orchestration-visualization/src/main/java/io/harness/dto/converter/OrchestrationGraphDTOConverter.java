/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dto.converter;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.OrchestrationGraph;
import io.harness.dto.OrchestrationGraphDTO;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationGraphDTOConverter {
  public OrchestrationGraphDTO convertFrom(OrchestrationGraph orchestrationGraph) {
    return OrchestrationGraphDTO.builder()
        .startTs(orchestrationGraph.getStartTs())
        .endTs(orchestrationGraph.getEndTs())
        .status(orchestrationGraph.getStatus())
        .rootNodeIds(orchestrationGraph.getRootNodeIds())
        .planExecutionId(orchestrationGraph.getPlanExecutionId())
        .adjacencyList(OrchestrationAdjacencyListDTOConverter.convertFrom(orchestrationGraph.getAdjacencyList()))
        .build();
  }

  public OrchestrationGraphDTO convertFrom(EphemeralOrchestrationGraph ephemeralOrchestrationGraph) {
    return OrchestrationGraphDTO.builder()
        .startTs(ephemeralOrchestrationGraph.getStartTs())
        .endTs(ephemeralOrchestrationGraph.getEndTs())
        .status(ephemeralOrchestrationGraph.getStatus())
        .rootNodeIds(ephemeralOrchestrationGraph.getRootNodeIds())
        .planExecutionId(ephemeralOrchestrationGraph.getPlanExecutionId())
        .adjacencyList(
            OrchestrationAdjacencyListDTOConverter.convertFrom(ephemeralOrchestrationGraph.getAdjacencyList()))
        .build();
  }
}
