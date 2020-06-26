package io.harness.engine.events;

import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrchestrationStartEventHandler implements OrchestrationEventHandler {
  @Override
  public void handleEvent(OrchestrationEvent event) {
    logger.info("Event Received: {}", event.getEventType());
  }
}
