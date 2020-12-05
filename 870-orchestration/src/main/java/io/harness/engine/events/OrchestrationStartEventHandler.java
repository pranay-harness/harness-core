package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
public class OrchestrationStartEventHandler implements AsyncOrchestrationEventHandler {
  @Override
  public void handleEvent(OrchestrationEvent event) {
    log.info("Event Received: {}", event.getEventType());
  }
}
