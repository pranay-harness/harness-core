package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
public enum NodeExecutionEntityType {
  STEP_PARAMETERS,
  OUTCOME,
  SWEEPING_OUTPUT;

  public static Set<NodeExecutionEntityType> allEntities() {
    return EnumSet.of(STEP_PARAMETERS, OUTCOME, SWEEPING_OUTPUT);
  }
}
