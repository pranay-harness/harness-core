package io.harness.pms.events.base;

import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.Map;

public class NoopSdkSdkBaseEventMessageListener extends PmsAbstractMessageListener<InterruptEvent> {
  public NoopSdkSdkBaseEventMessageListener(String serviceName) {
    super(serviceName, InterruptEvent.class);
  }

  @Override
  public boolean processMessage(InterruptEvent event, Map<String, String> metadataMap) {
    return false;
  }
}
