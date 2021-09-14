/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

/**
 * Created by rsingh on 7/25/17.
 */
public enum AnalysisComparisonStrategy {
  COMPARE_WITH_PREVIOUS("Compare with previous run"),
  COMPARE_WITH_CURRENT("Compare with current run"),
  PREDICTIVE("Compare with Prediction based on history");

  private final String name;

  AnalysisComparisonStrategy(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
