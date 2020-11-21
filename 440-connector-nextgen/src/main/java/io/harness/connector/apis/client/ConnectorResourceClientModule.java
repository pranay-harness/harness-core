package io.harness.connector.apis.client;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class ConnectorResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public ConnectorResourceClientModule(
      ServiceHttpClientConfig secretManagerConfig, String serviceSecret, String clientId) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private ConnectorResourceHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ConnectorResourceHttpClientFactory(
        this.secretManagerConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(ConnectorResourceClient.class).toProvider(ConnectorResourceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
