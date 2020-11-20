package io.harness.executionplan;

import lombok.Getter;

public enum CIPlanCreatorType {
  EXECUTION_PLAN_CREATOR("EXECUTION_PLAN_CREATOR");

  @Getter private final String name;

  CIPlanCreatorType(String name) {
    this.name = name;
  }
}
