/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.tiserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.beans.entities.TIServiceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.CI)
public class TIServiceClientModule extends AbstractModule {
  TIServiceConfig tiServiceConfig;

  @Inject
  public TIServiceClientModule(TIServiceConfig tiServiceConfig) {
    this.tiServiceConfig = tiServiceConfig;
  }

  @Override
  protected void configure() {
    this.bind(TIServiceConfig.class).toInstance(this.tiServiceConfig);
    this.bind(TIServiceClient.class).toProvider(TIServiceClientFactory.class).in(Scopes.SINGLETON);
  }
}
