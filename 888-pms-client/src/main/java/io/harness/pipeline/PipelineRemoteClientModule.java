package io.harness.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pipeline.remote.PipelineServiceHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PIPELINE)
public class PipelineRemoteClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;
  public static final String SECRET_NG_MANAGER_CLIENT_SERVICE = "secretNGManagerClientService";

  public PipelineRemoteClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private PipelineServiceHttpClientFactory secretNGManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new PipelineServiceHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(PipelineServiceClient.class).toProvider(PipelineServiceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
