package io.harness.pms.events.base;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.Map;

public class NoopPmsEventHandler extends PmsBaseEventHandler<InterruptEvent> {
  @Override
  protected Map<String, String> extraLogProperties(InterruptEvent event) {
    return null;
  }

  @Override
  protected Ambiance extractAmbiance(InterruptEvent event) {
    return null;
  }

  @Override
  protected Map<String, String> extractMetricContext(InterruptEvent message) {
    return null;
  }

  @Override
  protected String getMetricPrefix(InterruptEvent message) {
    return null;
  }

  @Override
  protected void handleEventWithContext(InterruptEvent event) {}
}
