package io.harness.feature;

import io.harness.feature.services.FeatureService;
import io.harness.feature.services.FeaturesManagementJob;
import io.harness.feature.services.impl.FeatureServiceImpl;
import io.harness.feature.services.impl.FeaturesManagementJobImpl;

import com.google.inject.AbstractModule;

public class EnforcementModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(FeatureService.class).to(FeatureServiceImpl.class);
    bind(FeaturesManagementJob.class).to(FeaturesManagementJobImpl.class);
  }
}
