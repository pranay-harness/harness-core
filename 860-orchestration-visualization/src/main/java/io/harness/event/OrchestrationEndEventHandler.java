package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

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

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEndEventHandler implements AsyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  public void handleEvent(OrchestrationEvent event) {
    try {
      Ambiance ambiance = event.getAmbiance();
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
      // One last time try to update the graph to process any unprocessed logs
      graphGenerationService.updateGraph(planExecution.getUuid());

      log.info("Ending Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      OrchestrationGraph orchestrationGraph =
          graphGenerationService.getCachedOrchestrationGraph(ambiance.getPlanExecutionId());
      orchestrationGraph = orchestrationGraph.withStatus(planExecution.getStatus()).withEndTs(planExecution.getEndTs());
      graphGenerationService.cacheOrchestrationGraph(orchestrationGraph);
    } catch (Exception e) {
      log.error("Cannot update Orchestration graph for ORCHESTRATION_END");
      throw e;
    }
  }
}
