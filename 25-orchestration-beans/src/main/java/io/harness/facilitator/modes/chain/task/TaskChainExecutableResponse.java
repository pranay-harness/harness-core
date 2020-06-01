package io.harness.facilitator.modes.chain.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.TaskSpawningExecutableResponse;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
public class TaskChainExecutableResponse implements TaskSpawningExecutableResponse {
  @NonNull String taskId;
  @NonNull String taskIdentifier;
  @NonNull String taskType;
  boolean chainEnd;
  PassThroughData passThroughData;
}
