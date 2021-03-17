package io.harness.auditclient.remote;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class AuditClientModule extends AbstractModule {
  private final ServiceHttpClientConfig auditClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public AuditClientModule(ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this.auditClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private AuditClientFactory auditClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new AuditClientFactory(
        auditClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(AuditClient.class).toProvider(AuditClientFactory.class).in(Scopes.SINGLETON);
  }
}
