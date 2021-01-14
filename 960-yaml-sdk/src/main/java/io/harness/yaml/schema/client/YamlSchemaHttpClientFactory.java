package io.harness.yaml.schema.client;

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
public class YamlSchemaHttpClientFactory extends AbstractHttpClientFactory implements Provider<YamlSchemaClient> {
  public YamlSchemaHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
    log.info("secretManagerConfig {}", secretManagerConfig);
  }

  @Override
  public YamlSchemaClient get() {
    return getRetrofit().create(YamlSchemaClient.class);
  }
}
