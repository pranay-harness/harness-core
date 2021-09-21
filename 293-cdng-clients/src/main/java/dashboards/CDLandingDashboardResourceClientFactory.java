package dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

@OwnedBy(PIPELINE)
public class CDLandingDashboardResourceClientFactory
    extends AbstractHttpClientFactory implements Provider<CDLandingDashboardResourceClient> {
  public CDLandingDashboardResourceClientFactory(ServiceHttpClientConfig config, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(config, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, ClientMode.PRIVILEGED);
  }

  @Override
  public CDLandingDashboardResourceClient get() {
    return getRetrofit().create(CDLandingDashboardResourceClient.class);
  }
}