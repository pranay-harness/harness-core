/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
