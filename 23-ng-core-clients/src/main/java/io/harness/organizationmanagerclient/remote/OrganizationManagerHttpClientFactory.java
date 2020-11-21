package io.harness.organizationmanagerclient.remote;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class OrganizationManagerHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<OrganizationManagerClient> {
  public OrganizationManagerHttpClientFactory(ServiceHttpClientConfig organizationManagerClientConfig,
      String serviceSecret, ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory,
      String clientId) {
    super(organizationManagerClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public OrganizationManagerClient get() {
    return getRetrofit().create(OrganizationManagerClient.class);
  }
}
