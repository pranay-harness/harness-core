package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.AsyncOrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class OrchestrationEndEventHandler implements AsyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    PlanExecution planExecution = planExecutionService.get(event.getAmbiance().getPlanExecutionId());
    log.info("Ending Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
        planExecution.getStatus());

    OrchestrationGraph cachedGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

    graphGenerationService.cacheOrchestrationGraph(
        cachedGraph.withStatus(planExecution.getStatus()).withEndTs(planExecution.getEndTs()));
  }
}
