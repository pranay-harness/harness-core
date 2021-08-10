package io.harness.feature.services;

import io.harness.feature.configs.FeatureInfo;
import io.harness.feature.interfaces.Feature;

public interface FeatureService {
  void registerFeature(String featureName, Feature feature);

  void initializeOrUpdateFeature(FeatureInfo featureInfo) throws NoSuchFieldException, IllegalAccessException;
}
