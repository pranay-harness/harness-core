package io.harness.remote;

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
public class NotificationHTTPFactory extends AbstractHttpClientFactory implements Provider<NotificationHTTPClient> {
  public NotificationHTTPFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(secretManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, "notification-client");
  }

  @Override
  public NotificationHTTPClient get() {
    return getRetrofit().create(NotificationHTTPClient.class);
  }
}
