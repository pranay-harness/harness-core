package io.harness.connector.apis.client;

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
public class ConnectorResourceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<ConnectorResourceClient> {
  public ConnectorResourceHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory);
  }

  @Override
  public ConnectorResourceClient get() {
    return getRetrofit().create(ConnectorResourceClient.class);
  }
}
