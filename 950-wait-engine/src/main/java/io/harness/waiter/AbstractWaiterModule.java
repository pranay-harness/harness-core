/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AbstractWaiterModule extends AbstractModule {
  protected void configure() {
    install(WaiterModule.getInstance());
  }

  @Provides
  @Singleton
  protected WaiterConfiguration injectWaiterConfiguration() {
    return waiterConfiguration();
  }

  public abstract WaiterConfiguration waiterConfiguration();
}
