package io.harness.engine.interrupts.handlers.publisher;

import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptType;

public interface InterruptEventPublisher {
  String publishEvent(String uuid, Interrupt interrupt, InterruptType interruptType);
}
