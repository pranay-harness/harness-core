package io.harness.engine.executables;

import io.harness.tasks.TaskMode;

public interface TaskExecuteStrategy extends ExecuteStrategy {
  TaskMode getMode();
}
