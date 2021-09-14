package io.harness.feature.services;

import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;

import java.util.List;

public interface FeatureService {
  boolean isFeatureAvailable(FeatureRestriction featureName, String accountIdentifier);
  void checkAvailabilityOrThrow(FeatureRestriction featureName, String accountIdentifier);
  void checkAvailabilityWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentUsage);
  FeatureDetailsDTO getFeatureDetail(FeatureRestriction featureName, String accountIdentifier);
  List<FeatureDetailsDTO> getEnabledFeatureDetails(String accountIdentifier);
}
