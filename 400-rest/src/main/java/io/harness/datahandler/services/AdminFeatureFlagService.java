/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.services;

import io.harness.beans.FeatureFlag;

import java.util.List;
import java.util.Optional;

public interface AdminFeatureFlagService {
  List<FeatureFlag> getAllFeatureFlags();

  Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag);

  Optional<FeatureFlag> getFeatureFlag(String featureFlagName);
}
