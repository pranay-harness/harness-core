package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public enum ExecutionMode {
  SYNC,
  ASYNC,
  SKIP,
  TASK_CHAIN,
  TASK_CHAIN_V2,
  CHILDREN,
  CHILD,
  TASK,
  CHILD_CHAIN,
  TASK_V2
}
