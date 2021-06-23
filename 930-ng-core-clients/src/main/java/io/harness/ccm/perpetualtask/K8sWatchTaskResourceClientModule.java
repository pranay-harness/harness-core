package io.harness.ccm.perpetualtask;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class K8sWatchTaskResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig httpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public K8sWatchTaskResourceClientModule(
      ServiceHttpClientConfig httpClientConfig, String serviceSecret, String clientId) {
    this.httpClientConfig = httpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private K8sWatchTaskResourceHttpClientFactory secretManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new K8sWatchTaskResourceHttpClientFactory(
        this.httpClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(K8sWatchTaskResourceClient.class)
        .toProvider(K8sWatchTaskResourceHttpClientFactory.class)
        .in(Scopes.SINGLETON);
  }
}
