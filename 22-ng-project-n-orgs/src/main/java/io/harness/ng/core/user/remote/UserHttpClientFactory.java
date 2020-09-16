package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(PL)
public class UserHttpClientFactory extends AbstractHttpClientFactory implements Provider<UserClient> {
  public UserHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory);
  }

  @Override
  public UserClient get() {
    return getRetrofit().create(UserClient.class);
  }
}
