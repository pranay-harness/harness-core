package io.harness.pms.sdk.core.execution.events.base;

import io.harness.pms.contracts.interrupts.InterruptEvent;

public class NoopSdkSdkBaseEventMessageListener extends SdkBaseEventMessageListener<InterruptEvent> {
  public NoopSdkSdkBaseEventMessageListener() {
    super(InterruptEvent.class);
  }

  @Override
  public boolean processMessage(InterruptEvent event) {
    return false;
  }
}
