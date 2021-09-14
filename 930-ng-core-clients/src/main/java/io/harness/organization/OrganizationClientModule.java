/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.organization;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.organization.remote.OrganizationClient;
import io.harness.organization.remote.OrganizationHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

@OwnedBy(PL)
public class OrganizationClientModule extends AbstractModule {
  private final ServiceHttpClientConfig organizationManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public OrganizationClientModule(
      ServiceHttpClientConfig organizationManagerClientConfig, String serviceSecret, String clientId) {
    this.organizationManagerClientConfig = organizationManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Named("PRIVILEGED")
  private OrganizationHttpClientFactory privilegedOrganizationHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new OrganizationHttpClientFactory(organizationManagerClientConfig, serviceSecret,
        new ServiceTokenGenerator(), kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private OrganizationHttpClientFactory nonPrivilegedOrganizationHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new OrganizationHttpClientFactory(organizationManagerClientConfig, serviceSecret,
        new ServiceTokenGenerator(), kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(OrganizationClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(OrganizationHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(OrganizationClient.class)
        .toProvider(Key.get(OrganizationHttpClientFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
