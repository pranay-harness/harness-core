package io.harness.delegatelog.client;

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
public class DelegateSelectionLogHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<DelegateSelectionLogHttpClient> {
  public DelegateSelectionLogHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public DelegateSelectionLogHttpClient get() {
    return getRetrofit().create(DelegateSelectionLogHttpClient.class);
  }
}
