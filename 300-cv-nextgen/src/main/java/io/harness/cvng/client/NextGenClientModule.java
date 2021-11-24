package io.harness.cvng.client;

import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class NextGenClientModule extends AbstractModule {
  private NGManagerServiceConfig ngManagerServiceConfig;

  public NextGenClientModule(NGManagerServiceConfig ngManagerServiceConfig) {
    this.ngManagerServiceConfig = ngManagerServiceConfig;
  }

  @Override
  protected void configure() {
    bind(NextGenClient.class)
        .toProvider(new NextGenClientFactory(ngManagerServiceConfig, new ServiceTokenGenerator()))
        .in(Singleton.class);
  }
}
