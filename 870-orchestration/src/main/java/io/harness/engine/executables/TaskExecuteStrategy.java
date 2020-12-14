package io.harness.engine.executables;

import io.harness.pms.contracts.execution.TaskMode;

public interface TaskExecuteStrategy extends ExecuteStrategy {
  TaskMode getMode();
}
