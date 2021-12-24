package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.core.utils.Thresholds;

public enum ErrorBudgetRisk {
  HEALTHY,
  OBSERVE,
  NEED_ATTENTION,
  UNHEALTHY,
  EXHAUSTED;
  public static ErrorBudgetRisk getFromPercentage(double errorBudgetRemainingPercentage) {
    if (errorBudgetRemainingPercentage >= Thresholds.HEALTHY_PERCENTAGE) {
      return ErrorBudgetRisk.HEALTHY;
    } else if (errorBudgetRemainingPercentage >= Thresholds.NEED_ATTENTION_PERCENTAGE) {
      return ErrorBudgetRisk.NEED_ATTENTION;
    } else if (errorBudgetRemainingPercentage >= Thresholds.OBSERVE_PERCENTAGE) {
      return ErrorBudgetRisk.OBSERVE;
    } else if (errorBudgetRemainingPercentage >= Thresholds.UNHEALTHY_PERCENTAGE) {
      return ErrorBudgetRisk.UNHEALTHY;
    } else {
      return ErrorBudgetRisk.EXHAUSTED;
    }
  }
}
