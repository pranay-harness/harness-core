/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.project;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.project.remote.ProjectClient;
import io.harness.project.remote.ProjectHttpClientFactory;
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
public class ProjectClientModule extends AbstractModule {
  private final ServiceHttpClientConfig projectManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public ProjectClientModule(
      ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this.projectManagerClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Named("PRIVILEGED")
  private ProjectHttpClientFactory privilegedProjectHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ProjectHttpClientFactory(projectManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private ProjectHttpClientFactory nonPrivilegedProjectHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ProjectHttpClientFactory(projectManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(ProjectClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(ProjectHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(ProjectClient.class)
        .toProvider(Key.get(ProjectHttpClientFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
