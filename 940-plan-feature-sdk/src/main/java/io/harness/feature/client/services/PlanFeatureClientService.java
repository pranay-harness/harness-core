package io.harness.feature.client.services;

import io.harness.feature.constants.FeatureRestriction;

public interface PlanFeatureClientService {
  boolean isAvailable(FeatureRestriction featureName, String accountIdentifier);
  void checkAvailability(FeatureRestriction featureName, String accountIdentifier);
}
