package software.wings.app;

import com.google.inject.AbstractModule;

import software.wings.licensing.violations.FeatureViolationsService;
import software.wings.licensing.violations.FeatureViolationsServiceImpl;

public class FeatureViolationsModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(FeatureViolationsService.class).to(FeatureViolationsServiceImpl.class);
  }
}
