package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.core.utils.Thresholds;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorBudgetRisk {
  HEALTHY("Healthy"),
  OBSERVE("Observe"),
  NEED_ATTENTION("Need Attention"),
  UNHEALTHY("Unhealthy"),
  EXHAUSTED("Exhausted");

  private String displayName;

  public static ErrorBudgetRisk getFromPercentage(double errorBudgetRemainingPercentage) {
    if (errorBudgetRemainingPercentage >= Thresholds.HEALTHY_PERCENTAGE) {
      return ErrorBudgetRisk.HEALTHY;
    } else if (errorBudgetRemainingPercentage >= Thresholds.OBSERVE_PERCENTAGE) {
      return ErrorBudgetRisk.OBSERVE;
    } else if (errorBudgetRemainingPercentage >= Thresholds.NEED_ATTENTION_PERCENTAGE) {
      return ErrorBudgetRisk.NEED_ATTENTION;
    } else if (errorBudgetRemainingPercentage >= Thresholds.UNHEALTHY_PERCENTAGE) {
      return ErrorBudgetRisk.UNHEALTHY;
    } else {
      return ErrorBudgetRisk.EXHAUSTED;
    }
  }
}
