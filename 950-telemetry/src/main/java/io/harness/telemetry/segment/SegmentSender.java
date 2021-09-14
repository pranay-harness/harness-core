/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry.segment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.TelemetryConfiguration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.GroupMessage;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class SegmentSender {
  private Analytics analytics;

  @Inject
  public SegmentSender(TelemetryConfiguration telemetryConfiguration) {
    if (!isValidConfig(telemetryConfiguration)) {
      log.warn("Failed to init SegmentSender due to disabled or invalid telemetryConfiguration");
      return;
    }
    try {
      analytics = Analytics.builder(telemetryConfiguration.getApiKey()).build();
    } catch (Exception ex) {
      log.error("Error while initializing Segment configuration", ex);
    }
  }

  public void enqueue(TrackMessage.Builder track) {
    if (isEnabled()) {
      analytics.enqueue(track);
      log.debug("Sent Track event to segment {}", track);
    } else {
      log.info("Skipping sending track event to segment");
    }
  }

  public void enqueue(GroupMessage.Builder group) {
    if (isEnabled()) {
      analytics.enqueue(group);
      log.debug("Sent Group event to segment {}", group);
    } else {
      log.info("Skipping sending group event to segment");
    }
  }

  public void enqueue(IdentifyMessage.Builder identity) {
    if (isEnabled()) {
      analytics.enqueue(identity);
      log.debug("Sent Identity event to segment {}", identity);
    } else {
      log.info("Skipping sending identity to segment");
    }
  }

  public boolean isEnabled() {
    return analytics != null;
  }

  private boolean isValidConfig(TelemetryConfiguration telemetryConfiguration) {
    return telemetryConfiguration != null && telemetryConfiguration.isEnabled()
        && isNotEmpty(telemetryConfiguration.getApiKey());
  }
}
