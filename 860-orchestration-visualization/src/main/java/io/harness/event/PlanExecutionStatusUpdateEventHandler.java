package io.harness.event;

import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
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
    Ambiance ambiance = event.getAmbiance();
    try {
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
      log.info("Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      OrchestrationGraph cachedGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

      graphGenerationService.cacheOrchestrationGraph(cachedGraph.withStatus(planExecution.getStatus()));
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(),
          AmbianceUtils.obtainCurrentRuntimeId(ambiance), ambiance.getPlanExecutionId(), e);
    }
  }
}
