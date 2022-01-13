/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import io.harness.exception.WingsException;

/**
 * Created by sriram_parthasarathy on 10/26/17.
 */
public enum AnalysisTolerance {
  LOW("Very sensitive - even small deviations are flagged as anomalies", 1, 0.9),
  MEDIUM("Moderately sensitive - only moderate deviations are flagged as anomalies (Recommended)", 2, 0.75),
  HIGH("Least sensitive - only major deviations are flagged as anomalies", 3, 0.5);

  private final String name;
  private final int tolerance;
  private final double simThreshold;

  AnalysisTolerance(String name, int tolerance, double simThreshold) {
    this.name = name;
    this.tolerance = tolerance;
    this.simThreshold = simThreshold;
  }

  public String getName() {
    return name;
  }

  public int tolerance() {
    return tolerance;
  }

  public double simThreshold() {
    return simThreshold;
  }

  public static AnalysisTolerance fromInt(int i) {
    switch (i) {
      case 1:
        return AnalysisTolerance.LOW;
      case 2:
        return AnalysisTolerance.MEDIUM;
      case 3:
        return AnalysisTolerance.HIGH;
      default:
        throw new WingsException("Unknown analysis tolerance " + i);
    }
  }
}
