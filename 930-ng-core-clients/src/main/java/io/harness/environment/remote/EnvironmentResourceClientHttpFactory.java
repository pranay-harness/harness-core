package io.harness.environment.remote;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(PIPELINE)
public class EnvironmentResourceClientHttpFactory
    extends AbstractHttpClientFactory implements Provider<EnvironmentResourceClient> {
  public EnvironmentResourceClientHttpFactory(ServiceHttpClientConfig ngManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(ngManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
    log.info("secretManagerConfig {}", ngManagerConfig);
  }

  @Override
  public EnvironmentResourceClient get() {
    return getRetrofit().create(EnvironmentResourceClient.class);
  }
}
