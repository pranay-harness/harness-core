package io.harness.feature.services;

import io.harness.ModuleType;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.configs.FeatureName;

import java.util.List;
import java.util.Set;

public interface FeatureService {
  boolean isFeatureAvailable(FeatureName featureName, String accountIdentifier);
  void checkAvailabilityOrThrow(FeatureName featureName, String accountIdentifier);
  FeatureDetailsDTO getFeatureDetail(FeatureName featureName, String accountIdentifier);
  List<FeatureDetailsDTO> getEnabledFeatureDetails(String accountIdentifier, ModuleType moduleType);
  Set<FeatureName> getAllFeatureNames();
  boolean isLockRequired(FeatureName featureName, String accountIdentifier);
}
