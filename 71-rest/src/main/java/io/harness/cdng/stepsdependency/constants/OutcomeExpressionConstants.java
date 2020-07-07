package io.harness.cdng.stepsdependency.constants;

import lombok.Getter;

public enum OutcomeExpressionConstants {
  SERVICE("service"),
  INFRASTRUCTURE("infrastructure"),
  ARTIFACTS("ARTIFACTS"),
  K8S_ROLL_OUT("rollingOutcome");

  @Getter private final String name;

  OutcomeExpressionConstants(String name) {
    this.name = name;
  }
}
