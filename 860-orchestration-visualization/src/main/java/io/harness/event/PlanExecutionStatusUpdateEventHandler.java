package io.harness.event;

import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PlanExecutionStatusUpdateEventHandler {
  @Inject private PlanExecutionService planExecutionService;

  public OrchestrationGraph handleEvent(OrchestrationEvent event, OrchestrationGraph orchestrationGraph) {
    Ambiance ambiance = event.getAmbiance();
    try {
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
      log.info("Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      return orchestrationGraph.withStatus(planExecution.getStatus());
    } catch (Exception e) {
      log.error("[{}] event failed for plan [{}]", event.getEventType(), ambiance.getPlanExecutionId(), e);
      throw e;
    }
  }
}
