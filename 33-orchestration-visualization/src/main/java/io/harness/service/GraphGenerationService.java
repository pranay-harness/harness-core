package io.harness.service;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Graph;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.dto.OrchestrationGraph;

@OwnedBy(HarnessTeam.CDC)
@Redesign
public interface GraphGenerationService {
  Graph generateGraph(String planExecutionId);
  OrchestrationGraphInternal getCachedOrchestrationGraphInternal(String planExecutionId);
  void cacheOrchestrationGraphInternal(OrchestrationGraphInternal adjacencyListInternal);
  OrchestrationGraph generateOrchestrationGraph(String planExecutionId);
  OrchestrationGraph generateOrchestrationGraphV2(String planExecutionId);
  OrchestrationGraph generatePartialOrchestrationGraph(String startingSetupNodeId, String planExecutionId);
}
