/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cf;

import io.harness.ff.FeatureFlagConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public abstract class AbstractCfModule extends AbstractModule {
  @Override
  protected void configure() {
    install(CfClientModule.getInstance());
  }

  @Provides
  @Singleton
  protected CfClientConfig injectCfClientConfig() {
    return cfClientConfig();
  };

  @Provides
  @Singleton
  protected CfMigrationConfig injectCfMigrationConfig() {
    return cfMigrationConfig();
  };

  @Provides
  @Singleton
  protected FeatureFlagConfig injectFeatureFlagConfig() {
    return featureFlagConfig();
  };

  public abstract CfClientConfig cfClientConfig();

  public abstract CfMigrationConfig cfMigrationConfig();

  public abstract FeatureFlagConfig featureFlagConfig();
}
