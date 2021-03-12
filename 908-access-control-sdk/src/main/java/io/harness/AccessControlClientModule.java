package io.harness;

import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.NGAccessControlCheckHandler;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlClientImpl;
import io.harness.accesscontrol.clients.AccessControlHttpClient;
import io.harness.accesscontrol.clients.AccessControlHttpClientFactory;
import io.harness.accesscontrol.clients.NoOpAccessControlClientImpl;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;

public class AccessControlClientModule extends AbstractModule {
  private static AccessControlClientModule instance;
  private final AccessControlClientConfiguration accessControlClientConfiguration;
  private final String clientId;

  private AccessControlClientModule(
      AccessControlClientConfiguration accessControlClientConfiguration, String clientId) {
    this.accessControlClientConfiguration = accessControlClientConfiguration;
    this.clientId = clientId;
  }

  public static synchronized AccessControlClientModule getInstance(
      AccessControlClientConfiguration accessControlClientConfiguration, String clientId) {
    if (instance == null) {
      instance = new AccessControlClientModule(accessControlClientConfiguration, clientId);
    }
    return instance;
  }

  @Provides
  private AccessControlHttpClientFactory accessControlHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new AccessControlHttpClientFactory(accessControlClientConfiguration.getAccessControlServiceConfig(),
        accessControlClientConfiguration.getAccessControlServiceSecret(), new ServiceTokenGenerator(),
        kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    if (accessControlClientConfiguration.isEnableAccessControl()) {
      bind(AccessControlHttpClient.class).toProvider(AccessControlHttpClientFactory.class).in(Scopes.SINGLETON);
      bind(AccessControlClient.class).to(AccessControlClientImpl.class).in(Scopes.SINGLETON);
    } else {
      bind(AccessControlClient.class).to(NoOpAccessControlClientImpl.class).in(Scopes.SINGLETON);
    }
    NGAccessControlCheckHandler ngAccessControlCheckHandler = new NGAccessControlCheckHandler();
    requestInjection(ngAccessControlCheckHandler);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(NGAccessControlCheck.class), ngAccessControlCheckHandler);
  }

  private void registerRequiredBindings() {}
}
