package io.harness.logserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.beans.entities.LogServiceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.CI)
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
