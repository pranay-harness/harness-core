package io.harness.delegate.app.modules;

import io.harness.delegate.app.health.DelegateHealthMonitor;

import com.google.inject.AbstractModule;

public class DelegateHealthModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateHealthMonitor.class).toInstance(new DelegateHealthMonitor());
  }
}
