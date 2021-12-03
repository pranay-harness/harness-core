package io.harness.cvng.analysis.beans;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;

public enum Risk {
  NO_DATA(-2),
  NO_ANALYSIS(-1),
  HEALTHY(0),
  OBSERVE(1),
  NEED_ATTENTION(2),
  UNHEALTHY(3);
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
    Integer healthScore = getHealthScoreFromRiskScore(riskScore);
    if (riskScore == -2.0) {
      return Risk.NO_DATA;
    } else if (riskScore < 0.0) {
      return Risk.NO_ANALYSIS;
    } else if (healthScore >= 75) {
      return Risk.HEALTHY;
    } else if (healthScore >= 50) {
      return Risk.OBSERVE;
    } else if (healthScore >= 25) {
      return Risk.NEED_ATTENTION;
    } else {
      return Risk.UNHEALTHY;
    }
  }

  public static Integer getHealthScoreFromRiskScore(double riskScore) {
    if (riskScore < 0) {
      return null;
    }
    return Integer.valueOf(100 - (int) Math.round(100 * riskScore));
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
