package io.harness.feature.client.services;

import io.harness.feature.client.PlanFeatureRegisterConfiguration;
import io.harness.feature.client.custom.CustomFeatureRestriction;
import io.harness.feature.client.usage.PlanFeatureUsageInterface;
import io.harness.feature.constants.FeatureRestriction;

public interface PlanFeatureRegisterService {
  void initialize(PlanFeatureRegisterConfiguration planFeatureClientConfiguration);
  PlanFeatureUsageInterface get(FeatureRestriction featureName);
  CustomFeatureRestriction getCustom(FeatureRestriction featureName);
}
