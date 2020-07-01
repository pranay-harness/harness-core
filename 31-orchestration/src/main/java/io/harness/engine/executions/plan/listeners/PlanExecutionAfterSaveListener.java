package io.harness.engine.executions.plan.listeners;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

@Slf4j
public class PlanExecutionAfterSaveListener extends AbstractMongoEventListener<PlanExecution> {
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public void onAfterSave(AfterSaveEvent<PlanExecution> event) {
    PlanExecution planExecution = event.getSource();
    if (event.getSource().getVersion() == 0) {
      eventEmitter.emitEvent(OrchestrationEvent.builder()
                                 .ambiance(Ambiance.builder()
                                               .planExecutionId(planExecution.getUuid())
                                               .setupAbstractions(planExecution.getSetupAbstractions())
                                               .build())
                                 .eventType(OrchestrationEventType.ORCHESTRATION_START)
                                 .build());
    }
  }
}
