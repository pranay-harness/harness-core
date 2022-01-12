/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
