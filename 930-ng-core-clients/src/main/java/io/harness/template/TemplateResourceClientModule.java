package io.harness.template;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.template.remote.TemplateResourceClientHttpFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class TemplateResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public TemplateResourceClientModule(
      ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private TemplateResourceClientHttpFactory templateResourceClientHttpFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new TemplateResourceClientHttpFactory(
        this.ngManagerClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(TemplateResourceClient.class).toProvider(TemplateResourceClientHttpFactory.class).in(Scopes.SINGLETON);
  }
}
