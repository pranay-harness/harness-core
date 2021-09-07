package io.harness.secrets.remote;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class SecretNGManagerHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<SecretNGManagerClient> {
  public SecretNGManagerHttpClientFactory(ServiceHttpClientConfig config, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      ClientMode clientMode) {
    super(config, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, clientMode);
  }

  @Override
  public SecretNGManagerClient get() {
    return getRetrofit().create(SecretNGManagerClient.class);
  }
}
