package io.harness.pms.sdk.core.execution;

import io.harness.pms.contracts.execution.TaskMode;

public interface TaskExecuteStrategy extends ExecuteStrategy {
  TaskMode getMode();
}
