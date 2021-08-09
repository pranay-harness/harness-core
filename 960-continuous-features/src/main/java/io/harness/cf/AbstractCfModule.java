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
