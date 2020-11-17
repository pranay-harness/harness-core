package io.harness.secrets;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.secrets.remote.SecretNGManagerHttpClientFactory;
import io.harness.secrets.services.SecretNGManagerClientServiceImpl;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class SecretNGManagerClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;
  public static final String SECRET_NG_MANAGER_CLIENT_SERVICE = "secretNGManagerClientService";

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
    bind(SecretManagerClientService.class)
        .annotatedWith(Names.named(SECRET_NG_MANAGER_CLIENT_SERVICE))
        .to(SecretNGManagerClientServiceImpl.class);
    bind(SecretNGManagerClient.class).toProvider(SecretNGManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
