package io.harness.ng.core;

import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.remote.UserHttpClientFactory;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.ng.core.user.services.api.impl.NgUserServiceImpl;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class UserClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public UserClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private UserHttpClientFactory userClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new UserHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(UserClient.class).toProvider(UserHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
