/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.NGAccessControlCheckHandler;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlHttpClient;
import io.harness.accesscontrol.clients.AccessControlHttpClientFactory;
import io.harness.accesscontrol.clients.NoOpAccessControlClientImpl;
import io.harness.accesscontrol.clients.NonPrivilegedAccessControlClientImpl;
import io.harness.accesscontrol.clients.PrivilegedAccessControlClientImpl;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ClientMode;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

@OwnedBy(PL)
public class AccessControlClientModule extends AbstractModule {
  private static AccessControlClientModule instance;
  private final AccessControlClientConfiguration accessControlClientConfiguration;
  private final String clientId;
  private final boolean enableCircuitBreaker;

  private AccessControlClientModule(AccessControlClientConfiguration accessControlClientConfiguration, String clientId,
      boolean enableCircuitBreaker) {
    this.accessControlClientConfiguration = accessControlClientConfiguration;
    this.clientId = clientId;
    this.enableCircuitBreaker = enableCircuitBreaker;
  }

  public static synchronized AccessControlClientModule getInstance(
      AccessControlClientConfiguration accessControlClientConfiguration, String clientId,
      boolean enableCircuitBreaker) {
    if (instance == null) {
      instance = new AccessControlClientModule(accessControlClientConfiguration, clientId, enableCircuitBreaker);
    }
    return instance;
  }

  public static synchronized AccessControlClientModule getInstance(
      AccessControlClientConfiguration accessControlClientConfiguration, String clientId) {
    if (instance == null) {
      instance = new AccessControlClientModule(accessControlClientConfiguration, clientId, false);
    }
    return instance;
  }

  private AccessControlHttpClientFactory privilegedAccessControlHttpClientFactory() {
    return new AccessControlHttpClientFactory(accessControlClientConfiguration.getAccessControlServiceConfig(),
        accessControlClientConfiguration.getAccessControlServiceSecret(), new ServiceTokenGenerator(), null, clientId,
        enableCircuitBreaker, ClientMode.PRIVILEGED);
  }

  private AccessControlHttpClientFactory nonPrivilegedAccessControlHttpClientFactory() {
    return new AccessControlHttpClientFactory(accessControlClientConfiguration.getAccessControlServiceConfig(),
        accessControlClientConfiguration.getAccessControlServiceSecret(), new ServiceTokenGenerator(), null, clientId,
        enableCircuitBreaker, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    if (accessControlClientConfiguration.isEnableAccessControl()) {
      bind(AccessControlHttpClient.class)
          .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
          .toProvider(privilegedAccessControlHttpClientFactory())
          .in(Scopes.SINGLETON);

      bind(AccessControlHttpClient.class)
          .annotatedWith(Names.named(ClientMode.NON_PRIVILEGED.name()))
          .toProvider(nonPrivilegedAccessControlHttpClientFactory())
          .in(Scopes.SINGLETON);

      bind(AccessControlClient.class)
          .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
          .to(PrivilegedAccessControlClientImpl.class)
          .in(Scopes.SINGLETON);
      bind(AccessControlClient.class)
          .annotatedWith(Names.named(ClientMode.NON_PRIVILEGED.name()))
          .to(NonPrivilegedAccessControlClientImpl.class)
          .in(Scopes.SINGLETON);
      bind(AccessControlClient.class).to(NonPrivilegedAccessControlClientImpl.class).in(Scopes.SINGLETON);
    } else {
      bind(AccessControlClient.class).to(NoOpAccessControlClientImpl.class).in(Scopes.SINGLETON);
      bind(AccessControlClient.class)
          .annotatedWith(Names.named(ClientMode.NON_PRIVILEGED.name()))
          .to(NoOpAccessControlClientImpl.class)
          .in(Scopes.SINGLETON);
      bind(AccessControlClient.class)
          .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
          .to(NoOpAccessControlClientImpl.class)
          .in(Scopes.SINGLETON);
    }
    NGAccessControlCheckHandler ngAccessControlCheckHandler = new NGAccessControlCheckHandler();
    requestInjection(ngAccessControlCheckHandler);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(NGAccessControlCheck.class), ngAccessControlCheckHandler);
  }

  private void registerRequiredBindings() {}
}
