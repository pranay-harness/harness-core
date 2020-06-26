package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import io.harness.ng.core.remote.client.SecretManagerClient;
import io.harness.ng.core.remote.client.factory.SecretManagerHttpClientFactory;
import io.harness.ng.core.services.api.impl.NGSecretManagerImpl;
import io.harness.security.ServiceTokenGenerator;
import software.wings.service.intfc.security.SecretManager;

public class SecretManagementModule extends AbstractModule {
  private final SecretManagerClientConfig secretManagerConfig;

  public SecretManagementModule(SecretManagerClientConfig secretManagerConfig) {
    this.secretManagerConfig = secretManagerConfig;
  }

  @Override
  protected void configure() {
    bind(SecretManagerClient.class)
        .toProvider(new SecretManagerHttpClientFactory(secretManagerConfig, new ServiceTokenGenerator()))
        .in(Scopes.SINGLETON);
    bind(SecretManager.class).to(NGSecretManagerImpl.class);
  }
}