package io.harness.feature.client;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@Slf4j
public class PlanFeatureClientFactory extends AbstractHttpClientFactory implements Provider<PlanFeatureClient> {
  protected PlanFeatureClientFactory(ServiceHttpClientConfig ngManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(ngManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, ClientMode.PRIVILEGED);
  }

  @Override
  public PlanFeatureClient get() {
    return getRetrofit().create(PlanFeatureClient.class);
  }
}
