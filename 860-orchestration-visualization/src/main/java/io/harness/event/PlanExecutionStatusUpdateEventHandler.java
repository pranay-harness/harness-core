package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.Status;
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
      if (planExecution.getStatus() == Status.ERRORED) {
        // If plan Execution is ERRORED force generate the graph
        // TODO: Here we need to regenrate Plan Execution Summary too. Till we do not have that at least regenerate the
        // graph. So that pipeline is failed
        log.info("[PMS_GRAPH]  Got Errored execution regenerating the graph final time");
        return graphGenerationService.buildOrchestrationGraph(planExecutionId);
      }
      log.info("[PMS_GRAPH]  Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());
      if (planExecution.getEndTs() != null) {
        orchestrationGraph = orchestrationGraph.withEndTs(planExecution.getEndTs());
      }
      return orchestrationGraph.withStatus(planExecution.getStatus());
    } catch (Exception e) {
      log.error("[PMS_GRAPH] Graph update for PLAN_EXECUTION_UPDATE event failed for plan [{}]", planExecutionId, e);
      throw e;
    }
  }
}
