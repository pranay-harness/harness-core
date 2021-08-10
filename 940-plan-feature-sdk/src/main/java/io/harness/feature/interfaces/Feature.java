package io.harness.feature.interfaces;

import io.harness.ModuleType;
import io.harness.feature.constants.FeatureType;

public interface Feature<T> {
  T getRestriction(String accountIdentifier);
  boolean isAvailable(String accountIdentifier);
  String getName();
  FeatureType getType();
  ModuleType getModuleType();
}
