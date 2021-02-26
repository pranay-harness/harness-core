package io.harness.pms.plan.execution.handlers;

import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;

public class PlanStatusEventEmitterHandler implements AsyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    Status status = planExecutionService.get(ambiance.getPlanExecutionId()).getStatus();
    if (status == Status.SUCCEEDED) {
      emitEvent(ambiance, OrchestrationEventType.PLAN_EXECUTION_SUCCESS);
    } else if (StatusUtils.brokeStatuses().contains(status)) {
      emitEvent(ambiance, OrchestrationEventType.PLAN_EXECUTION_FAILED);
    } else if (status == Status.PAUSED) {
      emitEvent(ambiance, OrchestrationEventType.PLAN_EXECUTION_PAUSED);
    }
  }

  public void emitEvent(Ambiance ambiance, OrchestrationEventType orchestrationEventType) {
    eventEmitter.emitEvent(OrchestrationEvent.builder().ambiance(ambiance).eventType(orchestrationEventType).build());
  }
}
