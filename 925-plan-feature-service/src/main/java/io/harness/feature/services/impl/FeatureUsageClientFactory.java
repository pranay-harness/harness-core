package io.harness.feature.services.impl;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

public class FeatureUsageClientFactory extends AbstractHttpClientFactory implements Provider<FeatureUsageClient> {
  protected FeatureUsageClientFactory(ServiceHttpClientConfig ngManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(ngManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, ClientMode.PRIVILEGED);
  }

  @Override
  public FeatureUsageClient get() {
    return getRetrofit().create(FeatureUsageClient.class);
  }
}
