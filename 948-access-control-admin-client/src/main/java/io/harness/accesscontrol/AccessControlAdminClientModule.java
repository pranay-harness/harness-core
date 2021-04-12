package io.harness.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.PL)
public class AccessControlAdminClientModule extends AbstractModule {
  private static AccessControlAdminClientModule instance;
  private final AccessControlAdminClientConfiguration accessControlAdminClientConfiguration;
  private final String clientId;

  public AccessControlAdminClientModule(
      AccessControlAdminClientConfiguration accessControlAdminClientConfiguration, String clientId) {
    this.accessControlAdminClientConfiguration = accessControlAdminClientConfiguration;
    this.clientId = clientId;
  }

  public static synchronized AccessControlAdminClientModule getInstance(
      AccessControlAdminClientConfiguration accessControlAdminClientConfiguration, String clientId) {
    if (instance == null) {
      instance = new AccessControlAdminClientModule(accessControlAdminClientConfiguration, clientId);
    }
    return instance;
  }

  @Provides
  private AccessControlAdminHttpClientFactory accessControlHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new AccessControlAdminHttpClientFactory(
        accessControlAdminClientConfiguration.getAccessControlServiceConfig(),
        accessControlAdminClientConfiguration.getAccessControlServiceSecret(), new ServiceTokenGenerator(),
        kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(AccessControlAdminClient.class).toProvider(AccessControlAdminHttpClientFactory.class).in(Scopes.SINGLETON);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
