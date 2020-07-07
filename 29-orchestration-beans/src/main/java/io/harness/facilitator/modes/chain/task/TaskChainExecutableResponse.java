package io.harness.facilitator.modes.chain.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.TaskSpawningExecutableResponse;
import io.harness.tasks.TaskMode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
public class TaskChainExecutableResponse implements TaskSpawningExecutableResponse {
  @NonNull String taskId;
  @NonNull TaskMode taskMode;
  boolean chainEnd;
  PassThroughData passThroughData;
}
