package io.harness.enforcement.client.services;

import io.harness.enforcement.client.RestrictionUsageRegisterConfiguration;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

public interface EnforcementSdkRegisterService {
  void initialize(RestrictionUsageRegisterConfiguration restrictionUsageRegisterConfiguration);
  RestrictionUsageInterface get(FeatureRestrictionName featureRestrictionName);
}
