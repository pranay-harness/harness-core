package io.harness.pipeline.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PIPELINE)
public class PMSLandingDashboardResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public PMSLandingDashboardResourceClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private PMSLandingDashboardResourceClientFactory pmsLandingDashboardResourceClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new PMSLandingDashboardResourceClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(PMSLandingDashboardResourceClient.class)
        .toProvider(PMSLandingDashboardResourceClientFactory.class)
        .in(Scopes.SINGLETON);
  }
}
