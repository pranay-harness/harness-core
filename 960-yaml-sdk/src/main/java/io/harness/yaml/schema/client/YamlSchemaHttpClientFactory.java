package io.harness.yaml.schema.client;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@OwnedBy(DX)
public class YamlSchemaHttpClientFactory extends AbstractHttpClientFactory {
  public YamlSchemaHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
    log.info("secretManagerConfig {}", secretManagerConfig);
  }

  public YamlSchemaClient create() {
    return getRetrofit().create(YamlSchemaClient.class);
  }
}
