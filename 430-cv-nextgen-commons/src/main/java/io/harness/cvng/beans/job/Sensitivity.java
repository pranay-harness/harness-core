/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans.job;

public enum Sensitivity {
  LOW(1),
  MEDIUM(2),
  HIGH(3);
  private final int tolerance;

  Sensitivity(int tolerance) {
    this.tolerance = tolerance;
  }

  public int getTolerance() {
    return tolerance;
  }

  /**
   * Currently CDNG yaml and this can have different value.
   * Ex : It can be HIGH(CVNG) or High (CDNG)
   */
  public static Sensitivity getEnum(String stringValue) {
    switch (stringValue) {
      case "HIGH":
      case "High":
        return Sensitivity.HIGH;
      case "MEDIUM":
      case "Medium":
        return Sensitivity.MEDIUM;
      case "LOW":
      case "Low":
        return Sensitivity.LOW;
      default:
        throw new IllegalStateException("No enum mapping found for " + stringValue);
    }
  }
}
