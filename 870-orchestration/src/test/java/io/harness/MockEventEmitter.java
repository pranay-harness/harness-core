package io.harness;

import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;

public class MockEventEmitter extends OrchestrationEventEmitter {
  @Override
  public void emitEvent(OrchestrationEvent event) {
    // Do Nothing
  }
}
