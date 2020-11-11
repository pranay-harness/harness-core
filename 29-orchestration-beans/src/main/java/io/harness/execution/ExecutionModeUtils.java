package io.harness.execution;

import io.harness.pms.execution.ExecutionMode;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.Set;

import static io.harness.pms.execution.ExecutionMode.*;

@UtilityClass
public class ExecutionModeUtils {
  private final Set<ExecutionMode> CHAIN_MODES = EnumSet.of(TASK_CHAIN, TASK_CHAIN_V2, TASK_CHAIN_V3, CHILD_CHAIN);

  private final Set<ExecutionMode> PARENT_MODES = EnumSet.of(CHILD_CHAIN, CHILDREN, CHILD);

  public Set<ExecutionMode> chainModes() {
    return CHAIN_MODES;
  }

  public boolean isParentMode(ExecutionMode mode) {
    return PARENT_MODES.contains(mode);
  }

  public boolean isChainMode(ExecutionMode mode) {
    return CHAIN_MODES.contains(mode);
  }
}
