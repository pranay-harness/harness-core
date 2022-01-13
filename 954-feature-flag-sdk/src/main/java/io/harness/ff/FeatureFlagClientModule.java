/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ff;

import static io.harness.remote.client.ClientMode.NON_PRIVILEGED;
import static io.harness.remote.client.ClientMode.PRIVILEGED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(HarnessTeam.PL)
public class FeatureFlagClientModule extends AbstractModule {
  private static FeatureFlagClientModule instance;
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  private FeatureFlagClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  public static FeatureFlagClientModule getInstance(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    if (instance == null) {
      instance = new FeatureFlagClientModule(serviceHttpClientConfig, serviceSecret, clientId);
    }

    return instance;
  }

  private FeatureFlagClientHttpFactory privilegedFeatureFlagClientHttpFactory() {
    return new FeatureFlagClientHttpFactory(
        serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), null, clientId, false, PRIVILEGED);
  }

  private FeatureFlagClientHttpFactory nonPrivilegedFeatureFlagClientHttpFactory() {
    return new FeatureFlagClientHttpFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(), null,
        clientId, false, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(FeatureFlagsClient.class)
        .annotatedWith(Names.named(PRIVILEGED.name()))
        .toProvider(privilegedFeatureFlagClientHttpFactory());
    bind(FeatureFlagsClient.class).toProvider(nonPrivilegedFeatureFlagClientHttpFactory());
    bind(FeatureFlagsClient.class)
        .annotatedWith(Names.named(NON_PRIVILEGED.name()))
        .toProvider(nonPrivilegedFeatureFlagClientHttpFactory());
  }
}
