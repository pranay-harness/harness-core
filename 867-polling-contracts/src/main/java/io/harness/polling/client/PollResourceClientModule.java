package io.harness.polling.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.CDC)
public class PollResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public PollResourceClientModule(
      ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private PollResourceHttpClientFactory providesHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new PollResourceHttpClientFactory(this.ngManagerClientConfig, this.serviceSecret,
        new ServiceTokenGenerator(), kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Override
  protected void configure() {
    this.bind(PollingResourceClient.class).toProvider(PollResourceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
