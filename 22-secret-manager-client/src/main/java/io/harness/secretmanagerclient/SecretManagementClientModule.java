package io.harness.secretmanagerclient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.ng.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.remote.SecretManagerHttpClientFactory;
import io.harness.secretmanagerclient.services.SecretManagerClientServiceImpl;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class SecretManagementClientModule extends AbstractModule {
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;

  public SecretManagementClientModule(ServiceHttpClientConfig secretManagerConfig, String serviceSecret) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private SecretManagerHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new SecretManagerHttpClientFactory(
        secretManagerConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(SecretManagerClientService.class).to(SecretManagerClientServiceImpl.class);
    bind(SecretManagerClient.class).toProvider(SecretManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
