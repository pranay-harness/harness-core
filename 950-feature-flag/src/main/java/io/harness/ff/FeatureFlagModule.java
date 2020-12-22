package io.harness.ff;

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
    bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
  }
}
