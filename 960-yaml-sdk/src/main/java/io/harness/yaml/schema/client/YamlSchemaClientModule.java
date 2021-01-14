package io.harness.yaml.schema.client;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class YamlSchemaClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public YamlSchemaClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private YamlSchemaHttpClientFactory yamlSchemaHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new YamlSchemaHttpClientFactory(
        this.serviceHttpClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(YamlSchemaClient.class).toProvider(YamlSchemaHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
