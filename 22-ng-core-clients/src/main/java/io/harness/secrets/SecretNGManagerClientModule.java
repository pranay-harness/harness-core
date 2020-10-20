package io.harness.secrets;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.secrets.remote.SecretNGManagerHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class SecretNGManagerClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;

  public SecretNGManagerClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private SecretNGManagerHttpClientFactory secretNGManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new SecretNGManagerHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(SecretNGManagerClient.class).toProvider(SecretNGManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
