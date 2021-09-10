package io.harness.feature.interfaces;

import io.harness.ModuleType;
import io.harness.feature.beans.FeatureDetailsDTO;

public interface FeatureInterface {
  void checkAvailability(String accountIdentifier);
  FeatureDetailsDTO toFeatureDetails(String accountIdentifier);
  ModuleType getModuleType();
  boolean isEnabledFeature(String accountIdentifier);
}
