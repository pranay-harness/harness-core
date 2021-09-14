/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.interrupts.handlers.publisher;

import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptType;

public interface InterruptEventPublisher {
  String publishEvent(String uuid, Interrupt interrupt, InterruptType interruptType);
}
