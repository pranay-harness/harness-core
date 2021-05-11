package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.secrets.remote.SecretNGManagerHttpClientFactory;
import io.harness.secrets.services.SecretNGManagerClientServiceImpl;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PL)
public class SecretNGManagerClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public SecretNGManagerClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private SecretNGManagerHttpClientFactory secretNGManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new SecretNGManagerHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(SecretManagerClientService.class).to(SecretNGManagerClientServiceImpl.class);
    bind(SecretNGManagerClient.class).toProvider(SecretNGManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
