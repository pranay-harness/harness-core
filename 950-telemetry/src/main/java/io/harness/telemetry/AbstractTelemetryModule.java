/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
