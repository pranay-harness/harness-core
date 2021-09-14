/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delay;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AbstractOrchestrationDelayModule extends AbstractModule {
  @Override
  protected void configure() {
    install(OrchestrationDelayModule.getInstance());
  }

  @Provides
  @Singleton
  @Named("forNG")
  protected boolean forNGProvider() {
    return forNG();
  }

  public abstract boolean forNG();
}
