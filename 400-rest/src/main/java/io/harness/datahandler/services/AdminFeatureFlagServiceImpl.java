/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.services;

import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;

public class AdminFeatureFlagServiceImpl implements AdminFeatureFlagService {
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public List<FeatureFlag> getAllFeatureFlags() {
    return featureFlagService.getAllFeatureFlags();
  }

  @Override
  public Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag) {
    return featureFlagService.updateFeatureFlag(featureFlagName, featureFlag);
  }

  @Override
  public Optional<FeatureFlag> getFeatureFlag(String featureFlagName) {
    return featureFlagService.getFeatureFlag(FeatureName.valueOf(featureFlagName));
  }
}
