/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.modules.remote;

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

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class ModulesHttpClientFactory extends AbstractHttpClientFactory implements Provider<ModulesClient> {
  public ModulesHttpClientFactory(ServiceHttpClientConfig config, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(config, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public ModulesClient get() {
    return getRetrofit().create(ModulesClient.class);
  }
}
