package io.harness.cvng.client;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CVNGClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public CVNGClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private CVNGClientFactory cvngClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new CVNGClientFactory(
        serviceHttpClientConfig, serviceSecret, clientId, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(CVNGClient.class).toProvider(CVNGClientFactory.class);
  }
}
