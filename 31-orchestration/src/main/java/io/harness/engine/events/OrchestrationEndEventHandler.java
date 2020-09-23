package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.AsyncOrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
public class OrchestrationEndEventHandler implements AsyncOrchestrationEventHandler {
  @Override
  public void handleEvent(OrchestrationEvent event) {
    logger.info("Event Received: {}", event.getEventType());
  }
}
