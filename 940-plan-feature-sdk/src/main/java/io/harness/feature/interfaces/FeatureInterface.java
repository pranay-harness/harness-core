/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.interfaces;

import io.harness.ModuleType;
import io.harness.feature.beans.FeatureDetailsDTO;

public interface FeatureInterface {
  void checkAvailability(String accountIdentifier);
  FeatureDetailsDTO toFeatureDetails(String accountIdentifier);
  ModuleType getModuleType();
  boolean isEnabledFeature(String accountIdentifier);
}
