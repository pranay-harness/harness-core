package io.harness.engine.executions.plan.listeners;

import com.google.inject.Inject;

import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.execution.PlanExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

@Slf4j
public class PlanExecutionAfterSaveListener extends AbstractMongoEventListener<PlanExecution> {
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public void onAfterSave(AfterSaveEvent<PlanExecution> event) {
    // TODO (prashant) => Remove this Class Not needed explicitly emit events where ever needed
  }
}
