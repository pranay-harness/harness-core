package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class StepDetailsUpdateEventHandler {
  @Inject PmsGraphStepDetailsService pmsGraphStepDetailsService;

  public OrchestrationGraph handleEvent(
      String planExecutionId, String nodeExecutionId, OrchestrationGraph orchestrationGraph) {
    try {
      orchestrationGraph.getAdjacencyList()
          .getGraphVertexMap()
          .get(nodeExecutionId)
          .setStepDetails(pmsGraphStepDetailsService.getStepDetails(planExecutionId, nodeExecutionId));
      return orchestrationGraph;
    } catch (Exception e) {
      log.error("Graph update for Step Details update event failed for node [{}]", nodeExecutionId, e);
      throw e;
    }
  }
}
