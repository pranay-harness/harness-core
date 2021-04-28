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
