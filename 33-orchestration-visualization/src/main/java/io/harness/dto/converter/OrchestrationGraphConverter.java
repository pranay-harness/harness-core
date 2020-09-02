package io.harness.dto.converter;

import io.harness.beans.OrchestrationGraphInternal;
import io.harness.dto.OrchestrationGraph;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationGraphConverter {
  public OrchestrationGraph convertFrom(OrchestrationGraphInternal graphInternal) {
    return OrchestrationGraph.builder()
        .planExecutionId(graphInternal.getPlanExecutionId())
        .rootNodeId(graphInternal.getRootNodeId())
        .status(graphInternal.getStatus())
        .startTs(graphInternal.getStartTs())
        .endTs(graphInternal.getEndTs())
        .adjacencyList(graphInternal.getAdjacencyList())
        .build();
  }
}
