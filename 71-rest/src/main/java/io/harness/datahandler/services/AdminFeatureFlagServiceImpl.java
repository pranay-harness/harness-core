package io.harness.datahandler.services;

import com.google.inject.Inject;

import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

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
