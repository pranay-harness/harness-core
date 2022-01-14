package io.harness.delegate.app.modules;

import io.harness.delegate.app.health.DelegateHealthMonitor;
import io.harness.health.HealthMonitor;

import com.google.inject.AbstractModule;

public class DelegateHealthModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HealthMonitor.class).to(DelegateHealthMonitor.class);
  }
}
