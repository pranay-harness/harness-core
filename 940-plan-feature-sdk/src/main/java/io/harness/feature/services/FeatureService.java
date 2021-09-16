package io.harness.feature.services;

import io.harness.ModuleType;
import io.harness.feature.beans.FeatureDetailsDTO;

import java.util.List;
import java.util.Set;

public interface FeatureService {
  boolean isFeatureAvailable(String featureName, String accountIdentifier);
  void checkAvailabilityOrThrow(String featureName, String accountIdentifier);
  FeatureDetailsDTO getFeatureDetail(String featureName, String accountIdentifier);
  List<FeatureDetailsDTO> getEnabledFeatureDetails(String accountIdentifier, ModuleType moduleType);
  Set<String> getAllFeatureNames();
  boolean isLockRequired(String featureName, String accountIdentifier);
}
