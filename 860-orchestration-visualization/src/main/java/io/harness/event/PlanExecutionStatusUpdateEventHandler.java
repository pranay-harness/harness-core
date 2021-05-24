package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionStatusUpdateEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;

  public OrchestrationGraph handleEvent(String planExecutionId, OrchestrationGraph orchestrationGraph) {
    try {
      PlanExecution planExecution = planExecutionService.get(planExecutionId);
      log.info("Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());
      if (planExecution.getEndTs() != null) {
        orchestrationGraph = orchestrationGraph.withEndTs(planExecution.getEndTs());
      }
      return orchestrationGraph.withStatus(planExecution.getStatus());
    } catch (Exception e) {
      log.error("Graph update for PLAN_EXECUTION_UPDATE event failed for plan [{}]", planExecutionId, e);
      throw e;
    }
  }
}
