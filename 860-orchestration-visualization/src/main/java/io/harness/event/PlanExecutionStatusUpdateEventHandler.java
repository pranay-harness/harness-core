package io.harness.event;

import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlanExecutionStatusUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    PlanExecution planExecution = planExecutionService.get(event.getAmbiance().getPlanExecutionId());
    log.info(
        "Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(), planExecution.getStatus());

    OrchestrationGraph cachedGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

    graphGenerationService.cacheOrchestrationGraph(cachedGraph.withStatus(planExecution.getStatus()));
  }
}
