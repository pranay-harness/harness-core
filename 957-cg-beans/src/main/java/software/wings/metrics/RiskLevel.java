/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.metrics;

/**
 * Created by mike@ on 4/11/17.
 */
public enum RiskLevel {
  HIGH(2),
  MEDIUM(1),
  LOW(0),
  NA(-1);

  private int risk;

  RiskLevel(int risk) {
    this.risk = risk;
  }

  public static RiskLevel getRiskLevel(int risk) {
    RiskLevel riskLevel;
    switch (risk) {
      case -1:
        riskLevel = RiskLevel.NA;
        break;
      case 0:
        riskLevel = RiskLevel.LOW;
        break;
      case 1:
        riskLevel = RiskLevel.MEDIUM;
        break;
      case 2:
        riskLevel = RiskLevel.HIGH;
        break;
      default:
        throw new IllegalArgumentException("Unknown risk level " + risk);
    }
    return riskLevel;
  }

  public int getRisk() {
    return risk;
  }
}
