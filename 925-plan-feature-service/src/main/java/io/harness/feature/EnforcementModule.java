package io.harness.feature;

import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.services.FeatureLoader;
import io.harness.feature.services.FeatureService;
import io.harness.feature.services.impl.FeatureLoaderImpl;
import io.harness.feature.services.impl.FeatureServiceImpl;

import com.google.inject.AbstractModule;

public class EnforcementModule extends AbstractModule {
  private static EnforcementModule instance;

  private EnforcementModule() {}

  public static EnforcementModule getInstance() {
    if (instance == null) {
      instance = new EnforcementModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(FeatureService.class).to(FeatureServiceImpl.class);
    bind(FeatureLoader.class).to(FeatureLoaderImpl.class);
    bind(RestrictionHandlerFactory.class);
  }
}
