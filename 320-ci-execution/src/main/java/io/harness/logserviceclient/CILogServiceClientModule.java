package io.harness.logserviceclient;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

import io.harness.ci.beans.entities.LogServiceConfig;

public class CILogServiceClientModule extends AbstractModule {
  LogServiceConfig logServiceConfig;

  @Inject
  public CILogServiceClientModule(LogServiceConfig logServiceConfig) {
    this.logServiceConfig = logServiceConfig;
  }

  @Override
  protected void configure() {
    this.bind(LogServiceConfig.class).toInstance(this.logServiceConfig);
    this.bind(CILogServiceClient.class).toProvider(CILogServiceClientFactory.class).in(Scopes.SINGLETON);
  }
}
