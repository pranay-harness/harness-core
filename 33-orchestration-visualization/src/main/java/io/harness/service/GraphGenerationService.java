package io.harness.service;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.dto.OrchestrationGraphDTO;

@OwnedBy(HarnessTeam.CDC)
@Redesign
public interface GraphGenerationService {
  OrchestrationGraph getCachedOrchestrationGraph(String planExecutionId);
  void cacheOrchestrationGraph(OrchestrationGraph adjacencyListInternal);
  @Deprecated OrchestrationGraphDTO generateOrchestrationGraph(String planExecutionId);
  OrchestrationGraphDTO generateOrchestrationGraphV2(String planExecutionId);
  OrchestrationGraphDTO generatePartialOrchestrationGraphFromSetupNodeId(
      String startingSetupNodeId, String planExecutionId);
  OrchestrationGraphDTO generatePartialOrchestrationGraphFromIdentifier(String identifier, String planExecutionId);
}
