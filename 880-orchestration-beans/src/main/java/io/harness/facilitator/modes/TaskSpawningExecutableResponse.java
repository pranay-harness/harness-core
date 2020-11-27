package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.TaskMode;

@OwnedBy(CDC)
public interface TaskSpawningExecutableResponse extends ExecutableResponse {
  String getTaskId();

  TaskMode getTaskMode();
}
