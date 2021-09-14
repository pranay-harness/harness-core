/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.Collections;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopPmsEventHandler extends PmsBaseEventHandler<InterruptEvent> {
  @Override
  protected Map<String, String> extraLogProperties(InterruptEvent event) {
    return null;
  }

  @Override
  protected Ambiance extractAmbiance(InterruptEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected Map<String, String> extractMetricContext(Map<String, String> metadataMap, InterruptEvent message) {
    return Collections.emptyMap();
  }

  @Override
  protected String getMetricPrefix(InterruptEvent message) {
    return null;
  }

  @Override
  protected void handleEventWithContext(InterruptEvent event) {}
}
