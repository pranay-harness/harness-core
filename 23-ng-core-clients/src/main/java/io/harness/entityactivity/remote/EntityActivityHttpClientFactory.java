package io.harness.entityactivity.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.Provider;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

@OwnedBy(DX)
public class EntityActivityHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<EntityActivityClient> {
  public EntityActivityHttpClientFactory(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(ngManagerClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory);
  }

  @Override
  public EntityActivityClient get() {
    return getRetrofit().create(EntityActivityClient.class);
  }
}
