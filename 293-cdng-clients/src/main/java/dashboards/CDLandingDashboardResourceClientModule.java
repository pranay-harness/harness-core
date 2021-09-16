package dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PIPELINE)
public class CDLandingDashboardResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public CDLandingDashboardResourceClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private CDLandingDashboardResourceClientFactory cdLandingDashboardResourceClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new CDLandingDashboardResourceClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(CDLandingDashboardResourceClient.class)
        .toProvider(CDLandingDashboardResourceClientFactory.class)
        .in(Scopes.SINGLETON);
  }
}
