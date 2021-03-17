package io.harness.delegatelog.client;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class DelegateSelectionLogHttpClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public DelegateSelectionLogHttpClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private DelegateSelectionLogHttpClientFactory delegateSelectionLogClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new DelegateSelectionLogHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(DelegateSelectionLogHttpClient.class)
        .toProvider(DelegateSelectionLogHttpClientFactory.class)
        .in(Scopes.SINGLETON);
  }
}