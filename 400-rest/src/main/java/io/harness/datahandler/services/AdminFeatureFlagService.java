package io.harness.datahandler.services;

import io.harness.beans.FeatureFlag;

import java.util.List;
import java.util.Optional;

public interface AdminFeatureFlagService {
  List<FeatureFlag> getAllFeatureFlags();

  Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag);

  Optional<FeatureFlag> getFeatureFlag(String featureFlagName);
}
