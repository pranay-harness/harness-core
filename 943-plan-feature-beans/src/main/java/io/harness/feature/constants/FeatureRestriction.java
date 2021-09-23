package io.harness.feature.constants;

import lombok.Getter;

/**
 * Please register all feature names here.
 */
public enum FeatureRestriction {
  // Test purpose
  TEST1,
  TEST2,
  TEST3,
  TEST4(FeatureType.CUSTOM);

  // All Features

  FeatureRestriction() {
    featureType = FeatureType.YAML;
  }

  FeatureRestriction(FeatureType featureType) {
    this.featureType = featureType;
  }

  @Getter private FeatureType featureType;
}
