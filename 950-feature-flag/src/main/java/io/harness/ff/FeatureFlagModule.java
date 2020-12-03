package io.harness.ff;

import io.harness.ff.FeatureFlagService;
import io.harness.ff.FeatureFlagServiceImpl;

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
