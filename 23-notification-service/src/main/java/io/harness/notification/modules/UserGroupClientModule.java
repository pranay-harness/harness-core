package io.harness.notification.modules;

import io.harness.notification.remote.UserGroupClient;
import io.harness.notification.remote.UserGroupHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class UserGroupClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;

  public UserGroupClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private UserGroupHttpClientFactory userClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new UserGroupHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(UserGroupClient.class).toProvider(UserGroupHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
