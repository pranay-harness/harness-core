/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateProgressServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateProgressService;
import io.harness.service.intfc.DelegateSyncService;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceDriverModule extends AbstractModule {
  private static volatile DelegateServiceDriverModule instance;
  private final boolean disableDeserialization;

  public DelegateServiceDriverModule(boolean disableDeserialization) {
    this.disableDeserialization = disableDeserialization;
  }

  public static DelegateServiceDriverModule getInstance(boolean disableDeserialization) {
    if (instance == null) {
      instance = new DelegateServiceDriverModule(disableDeserialization);
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(DelegateSyncService.class).to(DelegateSyncServiceImpl.class);
    bind(DelegateAsyncService.class).to(DelegateAsyncServiceImpl.class);
    bind(DelegateProgressService.class).to(DelegateProgressServiceImpl.class);
    bind(boolean.class).annotatedWith(Names.named("disableDeserialization")).toInstance(disableDeserialization);
  }
}
