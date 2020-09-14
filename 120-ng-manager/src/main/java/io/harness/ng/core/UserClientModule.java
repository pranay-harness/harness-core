package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.remote.UserHttpClientFactory;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.ng.core.user.services.api.impl.NgUserServiceImpl;
import io.harness.ng.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class UserClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;

  public UserClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private UserHttpClientFactory userClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new UserHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(NgUserService.class).to(NgUserServiceImpl.class);
    bind(UserClient.class).toProvider(UserHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
