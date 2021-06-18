package io.harness.ccm.manager;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class CENextGenResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig httpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public CENextGenResourceClientModule(
      ServiceHttpClientConfig httpClientConfig, String serviceSecret, String clientId) {
    this.httpClientConfig = httpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private CENextGenResourceHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new CENextGenResourceHttpClientFactory(
        this.httpClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(CENextGenResourceClient.class).toProvider(CENextGenResourceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
