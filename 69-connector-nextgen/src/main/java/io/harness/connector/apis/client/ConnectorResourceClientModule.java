package io.harness.connector.apis.client;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.remote.client.ServiceHttpClientConfig;

import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class ConnectorResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;

  @Inject
  public ConnectorResourceClientModule(ServiceHttpClientConfig secretManagerConfig, String serviceSecret) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private ConnectorResourceHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ConnectorResourceHttpClientFactory(
        this.secretManagerConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    this.bind(ConnectorResourceClient.class).toProvider(ConnectorResourceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
