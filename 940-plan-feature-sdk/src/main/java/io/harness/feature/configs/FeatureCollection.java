/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.configs;

/**
 * Please register all feature names here.
 */
public enum FeatureCollection {
  // Test purpose
  TEST1,
  TEST2,
  TEST3;

  // All Features

  public static boolean contains(String featureName) {
    try {
      FeatureCollection.valueOf(featureName);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
