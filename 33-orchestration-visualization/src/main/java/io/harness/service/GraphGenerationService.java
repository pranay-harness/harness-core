package io.harness.service;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Graph;
import io.harness.beans.OrchestrationAdjacencyListInternal;
import io.harness.dto.OrchestrationGraph;

@OwnedBy(HarnessTeam.CDC)
@Redesign
public interface GraphGenerationService {
  Graph generateGraph(String planExecutionId);
  OrchestrationAdjacencyListInternal getCachedOrchestrationAdjacencyListInternal(String planExecutionId);
  void cacheOrchestrationAdjacencyListInternal(OrchestrationAdjacencyListInternal adjacencyListInternal);
  OrchestrationGraph generateOrchestrationGraph(String planExecutionId);
  OrchestrationGraph generatePartialOrchestrationGraph(String startingSetupNodeId, String planExecutionId);
}
