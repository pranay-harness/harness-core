package io.harness.factory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosingFactoryModule extends AbstractModule {
  private ClosingFactory closingFactory;

  public ClosingFactoryModule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Provides
  @Singleton
  public ClosingFactory closingFactory() {
    return closingFactory;
  }

  @Override
  protected void configure() {
    // Nothing to configure
  }
}
