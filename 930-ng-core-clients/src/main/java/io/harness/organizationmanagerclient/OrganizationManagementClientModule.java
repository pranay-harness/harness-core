package io.harness.organizationmanagerclient;

import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.organizationmanagerclient.remote.OrganizationManagerHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class OrganizationManagementClientModule extends AbstractModule {
  private final ServiceHttpClientConfig organizationManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public OrganizationManagementClientModule(
      ServiceHttpClientConfig organizationManagerClientConfig, String serviceSecret, String clientId) {
    this.organizationManagerClientConfig = organizationManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private OrganizationManagerHttpClientFactory organizationManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new OrganizationManagerHttpClientFactory(
        organizationManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(OrganizationManagerClient.class).toProvider(OrganizationManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
