/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.app;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceModule extends AbstractModule {
  private final DelegateServiceConfig config;

  /**
   * Delegate Service App Config.
   */
  public DelegateServiceModule(DelegateServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(new DelegateServiceClassicGrpcServerModule(config));
  }
}
