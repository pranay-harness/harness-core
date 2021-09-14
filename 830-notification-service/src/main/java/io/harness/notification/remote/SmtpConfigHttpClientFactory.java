/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(PL)
public class SmtpConfigHttpClientFactory extends AbstractHttpClientFactory implements Provider<SmtpConfigClient> {
  public SmtpConfigHttpClientFactory(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(serviceHttpClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, "Default");
  }

  @Override
  public SmtpConfigClient get() {
    return getRetrofit().create(SmtpConfigClient.class);
  }
}
