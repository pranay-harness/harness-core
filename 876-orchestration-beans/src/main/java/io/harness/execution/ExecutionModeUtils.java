package io.harness.execution;

import static io.harness.pms.contracts.execution.ExecutionMode.*;

import io.harness.pms.contracts.execution.ExecutionMode;

import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionModeUtils {
  private final Set<ExecutionMode> CHAIN_MODES = EnumSet.of(TASK_CHAIN, CHILD_CHAIN);

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
