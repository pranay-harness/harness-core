package io.harness.engine.executables;

import io.harness.pms.execution.TaskMode;

public interface TaskExecuteStrategy extends ExecuteStrategy {
  TaskMode getMode();
}
