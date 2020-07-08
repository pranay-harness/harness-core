package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.remote.client.rest.factory.SecretManagerHttpClientFactory;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.ng.core.services.api.NgSecretUsageService;
import io.harness.ng.core.services.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretUsageServiceImpl;
import io.harness.security.ServiceTokenGenerator;

public class SecretManagementModule extends AbstractModule {
  private final SecretManagerClientConfig secretManagerConfig;
  private final String serviceSecret;

  public SecretManagementModule(SecretManagerClientConfig secretManagerConfig, String serviceSecret) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    bind(SecretManagerClient.class)
        .toProvider(new SecretManagerHttpClientFactory(
            this.secretManagerConfig, this.serviceSecret, new ServiceTokenGenerator()))
        .in(Scopes.SINGLETON);
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(NgSecretUsageService.class).to(NGSecretUsageServiceImpl.class);
  }
}
