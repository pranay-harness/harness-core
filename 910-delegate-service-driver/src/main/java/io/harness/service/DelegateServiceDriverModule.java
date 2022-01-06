/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
  private final boolean enablePrimaryCheck;

  public DelegateServiceDriverModule(boolean disableDeserialization, boolean enablePrimaryCheck) {
    this.disableDeserialization = disableDeserialization;
    this.enablePrimaryCheck = enablePrimaryCheck;
  }

  public static DelegateServiceDriverModule getInstance(boolean disableDeserialization, boolean enablePrimaryCheck) {
    if (instance == null) {
      instance = new DelegateServiceDriverModule(disableDeserialization, enablePrimaryCheck);
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(DelegateSyncService.class).to(DelegateSyncServiceImpl.class);
    bind(DelegateAsyncService.class).to(DelegateAsyncServiceImpl.class);
    bind(DelegateProgressService.class).to(DelegateProgressServiceImpl.class);
    bind(boolean.class).annotatedWith(Names.named("disableDeserialization")).toInstance(disableDeserialization);
    bind(boolean.class).annotatedWith(Names.named("enablePrimaryCheck")).toInstance(enablePrimaryCheck);
  }
}
