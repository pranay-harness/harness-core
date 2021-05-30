package io.harness.pms.plan.execution.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.observer.Subject;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.notification.orchestration.observers.NotificationObserver;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;
import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanStatusEventEmitterHandler implements AsyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;

  @Getter private final Subject<NotificationObserver> planExecutionSubject = new Subject<>();

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    Status status = planExecutionService.get(ambiance.getPlanExecutionId()).getStatus();
    if (status == Status.SUCCEEDED) {
      planExecutionSubject.fireInform(NotificationObxxxxxxxx:onSuccess, ambiance);
    } else if (StatusUtils.brokeStatuses().contains(status)) {
      planExecutionSubject.fireInform(NotificationObxxxxxxxx:onFailure, ambiance);
    } else if (status == Status.PAUSED) {
      planExecutionSubject.fireInform(NotificationObxxxxxxxx:onPause, ambiance);
    }
  }
}
