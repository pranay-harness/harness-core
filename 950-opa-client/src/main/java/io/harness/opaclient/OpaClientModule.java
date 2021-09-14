/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.opaclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class OpaClientModule extends AbstractModule {
  private final String opaServiceBaseUrl;
  private final String jwtAuthSecret;

  public OpaClientModule(String opaServiceBaseUrl, String jwtAuthSecret) {
    this.opaServiceBaseUrl = opaServiceBaseUrl;
    this.jwtAuthSecret = jwtAuthSecret;
  }

  @Override
  public void configure() {
    bind(OpaServiceClient.class).toProvider(new OpaClientFactory(opaServiceBaseUrl, jwtAuthSecret));
  }
}
