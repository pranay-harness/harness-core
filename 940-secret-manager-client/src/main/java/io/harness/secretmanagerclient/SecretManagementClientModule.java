package io.harness.secretmanagerclient;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.remote.SecretManagerHttpClientFactory;
import io.harness.secretmanagerclient.services.SecretManagerClientServiceImpl;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class SecretManagementClientModule extends AbstractModule {
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;
  private final String clientId;

  public SecretManagementClientModule(
      ServiceHttpClientConfig secretManagerConfig, String serviceSecret, String clientId) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private SecretManagerHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new SecretManagerHttpClientFactory(
        secretManagerConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(SecretManagerClientService.class).to(SecretManagerClientServiceImpl.class);
    bind(SecretManagerClient.class).toProvider(SecretManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
