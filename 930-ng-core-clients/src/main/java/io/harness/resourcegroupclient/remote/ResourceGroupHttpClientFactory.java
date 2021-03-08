package io.harness.resourcegroupclient.remote;

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
public class ResourceGroupHttpClientFactory extends AbstractHttpClientFactory implements Provider<ResourceGroupClient> {
  public ResourceGroupHttpClientFactory(ServiceHttpClientConfig resourceGroupClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(resourceGroupClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public ResourceGroupClient get() {
    return getRetrofit().create(ResourceGroupClient.class);
  }
}
