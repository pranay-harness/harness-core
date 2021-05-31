package io.harness.delegate.app;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateServiceModule extends AbstractModule {
  private final DelegateServiceConfig config;
  public DelegateServiceModule(DelegateServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {}
}
