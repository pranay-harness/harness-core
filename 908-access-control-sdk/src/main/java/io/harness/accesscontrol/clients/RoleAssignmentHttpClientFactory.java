package io.harness.accesscontrol.clients;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

public class RoleAssignmentHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<RoleAssignmentClient> {
  protected RoleAssignmentHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      boolean enableCircuitBreaker, ClientMode clientMode) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, enableCircuitBreaker,
        clientMode);
  }

  @Override
  public RoleAssignmentClient get() {
    return getRetrofit().create(RoleAssignmentClient.class);
  }
}
