package io.harness.resourcegroupclient;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.resourcegroupclient.remote.ResourceGroupHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class ResourceGroupClientModule extends AbstractModule {
  private final ServiceHttpClientConfig resourceGroupClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public ResourceGroupClientModule(
      ServiceHttpClientConfig resourceGroupClientConfig, String serviceSecret, String clientId) {
    this.resourceGroupClientConfig = resourceGroupClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private ResourceGroupHttpClientFactory resourceGroupHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ResourceGroupHttpClientFactory(
        resourceGroupClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(ResourceGroupClient.class).toProvider(ResourceGroupHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
