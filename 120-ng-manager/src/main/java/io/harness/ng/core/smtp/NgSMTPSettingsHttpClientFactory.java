package io.harness.ng.core.smtp;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static io.harness.annotations.dev.HarnessTeam.PL;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(PL)
public class NgSMTPSettingsHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<NgSMTPSettingsHttpClient> {
  public NgSMTPSettingsHttpClientFactory(ServiceHttpClientConfig ngManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(ngManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, "NgSMTP-settings-client");
  }
  @Override
  public NgSMTPSettingsHttpClient get() {
    return getRetrofit().create(NgSMTPSettingsHttpClient.class);
  }
}
