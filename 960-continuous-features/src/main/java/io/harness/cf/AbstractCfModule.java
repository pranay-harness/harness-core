/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
