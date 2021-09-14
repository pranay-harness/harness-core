/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.threading.ExecutorModule;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class DashboardServiceModule extends AbstractModule {
  private final DashboardServiceConfig config;

  public DashboardServiceModule(DashboardServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(ExecutorModule.getInstance());
  }
}
