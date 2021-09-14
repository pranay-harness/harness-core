/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handler.impl;

import static io.harness.event.model.EventConstants.ACCOUNT_ID;
import static io.harness.event.model.EventConstants.IS_24X7_ENABLED;
import static io.harness.event.model.EventConstants.VERIFICATION_STATE_TYPE;
import static io.harness.event.model.EventType.CV_META_DATA;

import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.metrics.HarnessMetricRegistry;

import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.ContinuousVerificationService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hazelcast.util.Preconditions;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class VerificationEventHandler implements EventHandler {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Inject
  public VerificationEventHandler(EventListener eventListener) {
    registerEventHandlers(eventListener);
  }

  private void registerEventHandlers(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(CV_META_DATA));
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.getEventType()) {
      case CV_META_DATA:
        handleCVMetaDataEvent(event);
        break;
      default:
        log.error("Invalid event type, dropping event : [{}]", event);
    }
  }

  private void handleCVMetaDataEvent(Event event) {
    Map<String, String> properties = event.getEventData().getProperties();

    Preconditions.checkNotNull(properties.get(ACCOUNT_ID));
    Preconditions.checkNotNull(properties.get(VERIFICATION_STATE_TYPE));
    Preconditions.checkNotNull(properties.get(IS_24X7_ENABLED));

    harnessMetricRegistry.recordGaugeValue(VerificationConstants.CV_META_DATA,
        new String[] {properties.get(ACCOUNT_ID), properties.get(VERIFICATION_STATE_TYPE),
            String.valueOf(properties.get(IS_24X7_ENABLED))},
        event.getEventData().getValue());
  }
}
