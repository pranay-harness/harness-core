/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.invites;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.invites.remote.NgInviteClient;
import io.harness.invites.remote.NgInviteHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

@OwnedBy(PL)
public class NgInviteClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public NgInviteClientModule(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Named("PRIVILEGED")
  private NgInviteHttpClientFactory privilegedNgInviteHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new NgInviteHttpClientFactory(this.ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private NgInviteHttpClientFactory nonPrivilegedNgInviteHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new NgInviteHttpClientFactory(this.ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(NgInviteClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(NgInviteHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(NgInviteClient.class)
        .toProvider(Key.get(NgInviteHttpClientFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
