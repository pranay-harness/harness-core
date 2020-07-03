package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.remote.client.rest.factory.SecretManagerHttpClientFactory;
import io.harness.ng.core.services.api.impl.NGSecretManagerImpl;
import io.harness.security.ServiceTokenGenerator;
import software.wings.service.intfc.security.SecretManager;

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
    bind(SecretManager.class).to(NGSecretManagerImpl.class);
  }
}