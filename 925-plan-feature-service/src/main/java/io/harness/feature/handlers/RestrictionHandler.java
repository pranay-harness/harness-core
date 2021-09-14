package io.harness.feature.handlers;

import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;

public interface RestrictionHandler {
  RestrictionType getRestrictionType();
  void check(FeatureRestriction featureName, String accountIdentifier);
  void checkWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentCount);
  void fillRestrictionDTO(
      FeatureRestriction featureName, String accountIdentifier, FeatureDetailsDTO featureDetailsDTO);
}
