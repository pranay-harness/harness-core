package io.harness.pipeline.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

@OwnedBy(PIPELINE)
public class PMSLandingDashboardResourceClientFactory
    extends AbstractHttpClientFactory implements Provider<PMSLandingDashboardResourceClient> {
  public PMSLandingDashboardResourceClientFactory(ServiceHttpClientConfig config, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(config, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, ClientMode.PRIVILEGED);
  }

  @Override
  public PMSLandingDashboardResourceClient get() {
    return getRetrofit().create(PMSLandingDashboardResourceClient.class);
  }
}
