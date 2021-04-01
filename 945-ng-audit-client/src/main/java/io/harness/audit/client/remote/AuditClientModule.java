package io.harness.audit.client.remote;

import io.harness.audit.client.api.AuditClientService;
import io.harness.audit.client.api.impl.AuditClientServiceImpl;
import io.harness.audit.client.api.impl.NoopAuditClientServiceImpl;
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
  private boolean enableAuditClient;

  public AuditClientModule(ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this(projectManagerClientConfig, serviceSecret, clientId, true);
  }

  public AuditClientModule(ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId,
      boolean enableAuditClient) {
    this.auditClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
    this.enableAuditClient = enableAuditClient;
  }

  @Provides
  private AuditClientFactory auditClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new AuditClientFactory(
        auditClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(AuditClient.class).toProvider(AuditClientFactory.class).in(Scopes.SINGLETON);
    if (enableAuditClient) {
      bind(AuditClientService.class).to(AuditClientServiceImpl.class);
    } else {
      bind(AuditClientService.class).to(NoopAuditClientServiceImpl.class);
    }
  }
}
