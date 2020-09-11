package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
@Redesign
public enum ExecutionMode {
  SYNC,
  ASYNC,
  SKIP,
  TASK_CHAIN,
  TASK_CHAIN_V2,
  TASK_CHAIN_V3,
  CHILDREN,
  CHILD,
  TASK,
  CHILD_CHAIN,
  TASK_V2,
  TASK_V3;

  private static final Set<ExecutionMode> CHAIN_MODES =
      EnumSet.of(TASK_CHAIN, TASK_CHAIN_V2, TASK_CHAIN_V3, CHILD_CHAIN);

  public static Set<ExecutionMode> chainModes() {
    return CHAIN_MODES;
  }
}
