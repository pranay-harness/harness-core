package io.harness.facilitator.modes.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.TaskSpawningExecutableResponse;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
public class TaskExecutableResponse implements TaskSpawningExecutableResponse {
  String taskId;
  String taskIdentifier;
  String taskType;
}
