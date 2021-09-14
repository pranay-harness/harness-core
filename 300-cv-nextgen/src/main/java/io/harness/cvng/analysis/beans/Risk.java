/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;

public enum Risk {
  NO_DATA(-2),
  NO_ANALYSIS(-1),
  LOW(0),
  MEDIUM(1),
  HIGH(2);
  private static final Map<Integer, Risk> INT_TO_RISK_MAP = new HashMap<>();
  static {
    for (Risk r : Risk.values()) {
      INT_TO_RISK_MAP.put(r.value, r);
    }
  }

  private final int value;
  public int getValue() {
    return value;
  }
  Risk(int value) {
    this.value = value;
  }

  public static Risk valueOf(int risk) {
    Preconditions.checkState(INT_TO_RISK_MAP.containsKey(risk), "Invalid value of risk %s", risk);
    return INT_TO_RISK_MAP.get(risk);
  }

  public static Risk getRiskFromRiskScore(double riskScore) {
    if (riskScore == -2.0) {
      return Risk.NO_DATA;
    } else if (riskScore < 0.0) {
      return Risk.NO_ANALYSIS;
    } else if (riskScore < .3) {
      return Risk.LOW;
    } else if (riskScore < .5) {
      return Risk.MEDIUM;
    } else {
      return Risk.HIGH;
    }
  }

  public boolean isGreaterThanEq(Risk other) {
    return this.getValue() >= other.getValue();
  }

  public boolean isGreaterThan(Risk other) {
    return this.getValue() > other.getValue();
  }

  public boolean isLessThanEq(Risk other) {
    return this.getValue() <= other.getValue();
  }
}
