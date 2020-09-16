package io.harness.secretmanagerclient.remote;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class SecretManagerHttpClientFactory extends AbstractHttpClientFactory implements Provider<SecretManagerClient> {
  public SecretManagerHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory);
  }

  @Override
  public SecretManagerClient get() {
    return getRetrofit().create(SecretManagerClient.class);
  }
}
