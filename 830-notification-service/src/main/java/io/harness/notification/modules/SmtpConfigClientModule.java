/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.modules;

import io.harness.notification.remote.SmtpConfigClient;
import io.harness.notification.remote.SmtpConfigHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class SmtpConfigClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;

  public SmtpConfigClientModule(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private SmtpConfigHttpClientFactory smtpConfigClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new SmtpConfigHttpClientFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(SmtpConfigClient.class).toProvider(SmtpConfigHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
