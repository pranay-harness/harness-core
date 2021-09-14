/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.remote.admin;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AdminLicenseHttpClientModule extends AbstractModule {
  private final ServiceHttpClientConfig adminLicenseHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Provides
  private AdminLicenseHttpClientFactory adminLicenseHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new AdminLicenseHttpClientFactory(this.adminLicenseHttpClientConfig, this.serviceSecret,
        new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(AdminLicenseHttpClient.class).toProvider(AdminLicenseHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
