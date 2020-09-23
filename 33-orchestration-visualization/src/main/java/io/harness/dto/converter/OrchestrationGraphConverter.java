package io.harness.dto.converter;

import io.harness.beans.OrchestrationGraphInternal;
import io.harness.dto.OrchestrationGraph;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationGraphConverter {
  public OrchestrationGraph convertFrom(OrchestrationGraphInternal orchestrationGraphInternal) {
    return OrchestrationGraph.builder()
        .startTs(orchestrationGraphInternal.getStartTs())
        .endTs(orchestrationGraphInternal.getEndTs())
        .status(orchestrationGraphInternal.getStatus())
        .rootNodeId(orchestrationGraphInternal.getRootNodeId())
        .planExecutionId(orchestrationGraphInternal.getPlanExecutionId())
        .adjacencyList(orchestrationGraphInternal.getAdjacencyList())
        .build();
  }
}
