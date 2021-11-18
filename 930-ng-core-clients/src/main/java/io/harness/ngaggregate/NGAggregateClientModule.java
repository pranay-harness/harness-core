package io.harness.ngaggregate;

import io.harness.ngaggregate.remote.NGAggregateClient;
import io.harness.ngaggregate.remote.NGAggregateClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class NGAggregateClientModule extends AbstractModule {
  private static NGAggregateClientModule instance;
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public NGAggregateClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  public static NGAggregateClientModule getInstance(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    if (instance == null) {
      instance = new NGAggregateClientModule(serviceHttpClientConfig, serviceSecret, clientId);
    }

    return instance;
  }

  @Provides
  private NGAggregateClientFactory ngAggregateClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new NGAggregateClientFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Provides
  @Named("PRIVILEGED")
  private NGAggregateClientFactory privilegedNGAggregateClientFactory() {
    return new NGAggregateClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), null, clientId, ClientMode.PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(NGAggregateClient.class).toProvider(NGAggregateClientFactory.class).in(Scopes.SINGLETON);
    bind(NGAggregateClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(privilegedNGAggregateClientFactory())
        .in(Scopes.SINGLETON);
  }
}
