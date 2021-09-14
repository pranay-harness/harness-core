/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;

public class MockEventEmitter extends OrchestrationEventEmitter {
  @Override
  public void emitEvent(OrchestrationEvent event) {
    // Do Nothing
  }
}
