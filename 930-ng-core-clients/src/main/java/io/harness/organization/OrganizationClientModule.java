package io.harness.organization;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.organization.remote.OrganizationClient;
import io.harness.organization.remote.OrganizationHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PL)
public class OrganizationClientModule extends AbstractModule {
  private final ServiceHttpClientConfig organizationManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public OrganizationClientModule(
      ServiceHttpClientConfig organizationManagerClientConfig, String serviceSecret, String clientId) {
    this.organizationManagerClientConfig = organizationManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private OrganizationHttpClientFactory organizationManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new OrganizationHttpClientFactory(
        organizationManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(OrganizationClient.class).toProvider(OrganizationHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
