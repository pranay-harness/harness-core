package io.harness.pms.sdk.core.interrupt;

import io.harness.pms.contracts.interrupts.InterruptEvent;

public interface InterruptEventHandler {
  boolean handleEvent(InterruptEvent event);
}
