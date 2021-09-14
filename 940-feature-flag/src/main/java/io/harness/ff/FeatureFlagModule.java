/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ff;

import io.harness.lock.PersistentLockModule;

import com.google.inject.AbstractModule;

public class FeatureFlagModule extends AbstractModule {
  private static volatile FeatureFlagModule instance;

  private FeatureFlagModule() {}

  public static FeatureFlagModule getInstance() {
    if (instance == null) {
      instance = new FeatureFlagModule();
    }

    return instance;
  }

  @Override
  protected void configure() {
    install(PersistentLockModule.getInstance());
    bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
  }
}
