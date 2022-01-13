/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry;

import static io.harness.configuration.DeployVariant.DEPLOY_VERSION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.configuration.DeployVariant;
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
import io.harness.telemetry.segment.remote.RemoteSegmentClient;
import io.harness.telemetry.segment.remote.RemoteSegmentClientFactory;
import io.harness.telemetry.segment.remote.RemoteSegmentSender;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
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
    String deployVersion = System.getenv().get(DEPLOY_VERSION);
    if (DeployVariant.isCommunity(deployVersion)) {
      bind(TelemetryReporter.class).to(RemoteSegmentSender.class);
      bind(RemoteSegmentClient.class).toProvider(RemoteSegmentClientFactory.class).in(Scopes.SINGLETON);
    } else {
      bind(TelemetryReporter.class).to(SegmentReporterImpl.class);
      bind(SegmentSender.class);
    }

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
