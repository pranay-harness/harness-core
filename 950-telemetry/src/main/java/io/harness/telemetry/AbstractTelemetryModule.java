/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public abstract class AbstractTelemetryModule extends AbstractModule {
  @Override
  protected void configure() {
    install(TelemetryModule.getInstance());
  }

  @Provides
  @Singleton
  protected TelemetryConfiguration injectTelemetryConfiguration() {
    return telemetryConfiguration();
  }

  public abstract TelemetryConfiguration telemetryConfiguration();
}
