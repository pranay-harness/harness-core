package io.harness.tiserviceclient;

import io.harness.ci.beans.entities.TIServiceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class TIServiceModule extends AbstractModule {
  private TIServiceConfig tiServiceConfig;

  @Inject
  public TIServiceModule(TIServiceConfig tiServiceConfig) {
    this.tiServiceConfig = tiServiceConfig;
  }

  @Override
  protected void configure() {
    this.bind(TIServiceConfig.class).toInstance(this.tiServiceConfig);
  }
}
