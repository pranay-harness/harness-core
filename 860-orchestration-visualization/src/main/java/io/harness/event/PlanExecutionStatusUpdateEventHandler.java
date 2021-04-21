package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionStatusUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;

  public void handleEvent(OrchestrationEvent event) {
    try {
      Ambiance ambiance = event.getAmbiance();
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
      log.info("Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());
      OrchestrationGraph orchestrationGraph =
          graphGenerationService.getCachedOrchestrationGraph(ambiance.getPlanExecutionId());
      orchestrationGraph = orchestrationGraph.withStatus(planExecution.getStatus());
      graphGenerationService.cacheOrchestrationGraph(orchestrationGraph);
    } catch (Exception e) {
      log.error("Graph update for PLAN_EXECUTION_UPDATE event failed for plan [{}]",
          event.getAmbiance().getPlanExecutionId(), e);
      throw e;
    }
  }
}
