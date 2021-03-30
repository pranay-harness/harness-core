package io.harness.ng.authenticationsettings.remote;

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
public class AuthSettingsManagerClientFactory
    extends AbstractHttpClientFactory implements Provider<AuthSettingsManagerClient> {
  public AuthSettingsManagerClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, "auth-settings-manager-client");
  }

  @Override
  public AuthSettingsManagerClient get() {
    return getRetrofit().create(AuthSettingsManagerClient.class);
  }
}
