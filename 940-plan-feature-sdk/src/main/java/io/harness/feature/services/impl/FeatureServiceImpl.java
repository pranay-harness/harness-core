package io.harness.feature.services.impl;

import io.harness.feature.bases.Feature;
import io.harness.feature.services.FeatureService;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private Map<String, Feature> featureMap;

  public FeatureServiceImpl() {
    featureMap = new HashMap<>();
  }

  void registerFeature(String featureName, Feature feature) {
    featureMap.put(featureName, feature);
  }
}
