package io.harness.ccm.manager;

import static io.harness.annotations.dev.HarnessTeam.CE;

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
@OwnedBy(CE)
public class CENextGenResourceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<CENextGenResourceClient> {
  public CENextGenResourceHttpClientFactory(ServiceHttpClientConfig httpClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(httpClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public CENextGenResourceClient get() {
    return getRetrofit().create(CENextGenResourceClient.class);
  }
}
