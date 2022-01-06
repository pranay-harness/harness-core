/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
