/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
