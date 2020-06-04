package io.harness.engine.expressions.functors;

import java.util.EnumSet;
import java.util.Set;

public enum NodeExecutionEntityType {
  STEP_PARAMETERS,
  OUTCOME,
  SWEEPING_OUTPUT;

  public static Set<NodeExecutionEntityType> allEntities() {
    return EnumSet.of(STEP_PARAMETERS, OUTCOME, SWEEPING_OUTPUT);
  }
}
