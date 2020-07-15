package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.managerclient.KryoConverterFactory;
import io.harness.ng.core.remote.client.rest.factory.SecretManagerClient;
import io.harness.ng.core.remote.client.rest.factory.SecretManagerHttpClientFactory;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.ng.core.services.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretServiceImpl;
import io.harness.ng.core.services.api.impl.SecretManagerClientServiceImpl;
import io.harness.secretmanagerclient.SecretManagerClientService;
import io.harness.security.ServiceTokenGenerator;

public class SecretManagementModule extends AbstractModule {
  private final SecretManagerClientConfig secretManagerConfig;
  private final String serviceSecret;

  public SecretManagementModule(SecretManagerClientConfig secretManagerConfig, String serviceSecret) {
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
    bind(SecretManagerClient.class).toProvider(SecretManagerHttpClientFactory.class).in(Scopes.SINGLETON);
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(SecretManagerClientService.class).to(SecretManagerClientServiceImpl.class);
  }
}
