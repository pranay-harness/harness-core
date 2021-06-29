package io.harness.ccm.perpetualtask;

import io.harness.remote.client.AbstractHttpClientFactory;
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
public class K8sWatchTaskResourceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<K8sWatchTaskResourceClient> {
  public K8sWatchTaskResourceHttpClientFactory(ServiceHttpClientConfig httpClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(httpClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public K8sWatchTaskResourceClient get() {
    return getRetrofit().create(K8sWatchTaskResourceClient.class);
  }
}
