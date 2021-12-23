package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.core.utils.Thresholds;

public enum ErrorBudgetRisk {
  HEALTHY,
  OBSERVE,
  NEED_ATTENTION,
  UNHEALTHY;
  public static io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk getFromPercentage(
      double errorBudgetRemainingPercentage) {
    if (errorBudgetRemainingPercentage >= Thresholds.HEALTHY_PERCENTAGE) {
      return io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk.HEALTHY;
    } else if (errorBudgetRemainingPercentage >= Thresholds.NEED_ATTENTION_PERCENTAGE) {
      return io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk.NEED_ATTENTION;
    } else if (errorBudgetRemainingPercentage >= Thresholds.OBSERVE_PERCENTAGE) {
      return io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk.OBSERVE;
    } else {
      return io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk.UNHEALTHY;
    }
  }
}
