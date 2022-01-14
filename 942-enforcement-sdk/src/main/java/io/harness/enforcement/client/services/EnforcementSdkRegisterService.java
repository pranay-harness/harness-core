package io.harness.enforcement.client.services;

import io.harness.enforcement.client.CustomRestrictionRegisterConfiguration;
import io.harness.enforcement.client.RestrictionUsageRegisterConfiguration;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

public interface EnforcementSdkRegisterService {
  void initialize(RestrictionUsageRegisterConfiguration restrictionUsageRegisterConfiguration,
      CustomRestrictionRegisterConfiguration customRestrictionRegisterConfiguration);
  RestrictionUsageInterface getRestrictionUsageInterface(FeatureRestrictionName featureRestrictionName);
  CustomRestrictionInterface getCustomRestrictionInterface(FeatureRestrictionName featureRestrictionName);
}
