package io.harness.enforcement.client.services;

import io.harness.enforcement.constants.FeatureRestrictionName;

public interface EnforcementClientService {
  boolean isAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  void checkAvailability(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
}
