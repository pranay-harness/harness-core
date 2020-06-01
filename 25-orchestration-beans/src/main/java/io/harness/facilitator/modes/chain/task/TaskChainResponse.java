package io.harness.facilitator.modes.chain.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.tasks.Task;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class TaskChainResponse {
  boolean chainEnd;
  PassThroughData passThroughData;
  Task task;
}
