package io.harness.notification.remote;

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
public class UserGroupHttpClientFactory extends AbstractHttpClientFactory implements Provider<UserGroupClient> {
  public UserGroupHttpClientFactory(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(serviceHttpClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, "notification-service");
  }

  @Override
  public UserGroupClient get() {
    return getRetrofit().create(UserGroupClient.class);
  }
}
