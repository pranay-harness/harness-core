package io.harness.pricing.client;

import io.harness.remote.client.ServiceHttpClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class CloudInfoPricingClientModule extends AbstractModule {
  private final ServiceHttpClientConfig httpClientConfig;

  @Inject
  public CloudInfoPricingClientModule(ServiceHttpClientConfig httpClientConfig) {
    this.httpClientConfig = httpClientConfig;
  }

  @Provides
  private CloudInfoPricingHttpClientFactory providesHttpClientFactory() {
    return new CloudInfoPricingHttpClientFactory(this.httpClientConfig);
  }

  @Override
  protected void configure() {
    bind(CloudInfoPricingClient.class).toProvider(CloudInfoPricingHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
