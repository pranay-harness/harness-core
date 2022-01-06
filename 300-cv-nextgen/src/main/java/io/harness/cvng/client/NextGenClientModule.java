package io.harness.cvng.client;

import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.remote.client.ClientMode;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class NextGenClientModule extends AbstractModule {
  private static NextGenClientModule instance;
  private NGManagerServiceConfig ngManagerServiceConfig;

  public NextGenClientModule(NGManagerServiceConfig ngManagerServiceConfig) {
    this.ngManagerServiceConfig = ngManagerServiceConfig;
  }

  public static NextGenClientModule getInstance(NGManagerServiceConfig ngManagerServiceConfig) {
    if (instance == null) {
      instance = new NextGenClientModule(ngManagerServiceConfig);
    }
    return instance;
  }

  @Provides
  @Named("PRIVILEGED")
  private NextGenClientFactory privilegedNextGenClientFactory() {
    return new NextGenClientFactory(ngManagerServiceConfig, new ServiceTokenGenerator(), ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private NextGenClientFactory nonPrivilegedNextGenClientFactory() {
    return new NextGenClientFactory(ngManagerServiceConfig, new ServiceTokenGenerator(), ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(NextGenClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(privilegedNextGenClientFactory())
        .in(Scopes.SINGLETON);
    bind(NextGenClient.class)
        .annotatedWith(Names.named(ClientMode.NON_PRIVILEGED.name()))
        .toProvider(nonPrivilegedNextGenClientFactory())
        .in(Scopes.SINGLETON);
    bind(NextGenClient.class).toProvider(nonPrivilegedNextGenClientFactory()).in(Scopes.SINGLETON);
  }
}
