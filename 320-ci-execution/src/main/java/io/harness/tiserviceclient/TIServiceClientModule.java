package io.harness.tiserviceclient;

import io.harness.ci.beans.entities.TIServiceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

public class TIServiceClientModule extends AbstractModule {
  TIServiceConfig tiServiceConfig;

  @Inject
  public TIServiceClientModule(TIServiceConfig tiServiceConfig) {
    this.tiServiceConfig = tiServiceConfig;
  }

  @Override
  protected void configure() {
    this.bind(TIServiceConfig.class).toInstance(this.tiServiceConfig);
    this.bind(TIServiceClient.class).toProvider(TIServiceClientFactory.class).in(Scopes.SINGLETON);
  }
}
