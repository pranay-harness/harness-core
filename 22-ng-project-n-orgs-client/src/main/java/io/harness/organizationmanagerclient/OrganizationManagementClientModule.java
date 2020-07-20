package io.harness.organizationmanagerclient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.organizationmanagerclient.remote.OrganizationManagerHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class OrganizationManagementClientModule extends AbstractModule {
  private final OrganizationManagerClientConfig organizationManagerClientConfig;
  private final String serviceSecret;

  public OrganizationManagementClientModule(
      OrganizationManagerClientConfig organizationManagerClientConfig, String serviceSecret) {
    this.organizationManagerClientConfig = organizationManagerClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private OrganizationManagerHttpClientFactory organizationManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new OrganizationManagerHttpClientFactory(
        organizationManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(OrganizationManagerClient.class).toProvider(OrganizationManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
