/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderMethodInterceptor;
import io.harness.telemetry.annotation.GroupEventInterceptor;
import io.harness.telemetry.annotation.IdentifyEventInterceptor;
import io.harness.telemetry.annotation.SendGroupEvent;
import io.harness.telemetry.annotation.SendIdentifyEvent;
import io.harness.telemetry.annotation.SendTrackEvent;
import io.harness.telemetry.annotation.SendTrackEvents;
import io.harness.telemetry.annotation.TrackEventInterceptor;
import io.harness.telemetry.segment.SegmentReporterImpl;
import io.harness.telemetry.segment.SegmentSender;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

@OwnedBy(HarnessTeam.GTM)
public class TelemetryModule extends AbstractModule {
  private static TelemetryModule instance;

  private TelemetryModule() {}

  static TelemetryModule getInstance() {
    if (instance == null) {
      instance = new TelemetryModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(TelemetryReporter.class).to(SegmentReporterImpl.class);
    bind(SegmentSender.class);

    ProviderMethodInterceptor trackEventInterceptor =
        new ProviderMethodInterceptor(getProvider(TrackEventInterceptor.class));
    ProviderMethodInterceptor identifyEventInterceptor =
        new ProviderMethodInterceptor(getProvider(IdentifyEventInterceptor.class));
    ProviderMethodInterceptor groupEventInterceptor =
        new ProviderMethodInterceptor(getProvider(GroupEventInterceptor.class));

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(SendTrackEvent.class), trackEventInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(SendTrackEvents.class), trackEventInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(SendIdentifyEvent.class), identifyEventInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(SendGroupEvent.class), groupEventInterceptor);
  }
}
