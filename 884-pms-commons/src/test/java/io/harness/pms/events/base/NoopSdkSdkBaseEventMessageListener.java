package io.harness.pms.events.base;

import io.harness.pms.contracts.interrupts.InterruptEvent;

public class NoopSdkSdkBaseEventMessageListener extends PmsAbstractMessageListener<InterruptEvent> {
  public NoopSdkSdkBaseEventMessageListener(String serviceName) {
    super(serviceName, InterruptEvent.class);
  }

  @Override
  public boolean processMessage(InterruptEvent event) {
    return false;
  }
}
