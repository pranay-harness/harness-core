package io.harness.pms.events.base;

import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.concurrent.Executors;

public class NoopSdkSdkBaseEventMessageListener
    extends PmsAbstractMessageListener<InterruptEvent, NoopPmsEventHandler> {
  public NoopSdkSdkBaseEventMessageListener(String serviceName, NoopPmsEventHandler handler) {
    super(serviceName, InterruptEvent.class, handler, Executors.newSingleThreadExecutor());
  }
}
