package io.harness.facilitator.modes.chain.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.PassThroughData;
import io.harness.pms.sdk.core.data.Metadata;
import io.harness.tasks.Task;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
public class TaskChainResponse {
  boolean chainEnd;
  PassThroughData passThroughData;
  Task task;
  Metadata metadata;

  public static class TaskChainResponseBuilder {
    public TaskChainResponse build() {
      if (task == null && !chainEnd) {
        throw new InvalidRequestException("Task Cannot be null if not chain end");
      }
      return internalBuild();
    }
  }
}
