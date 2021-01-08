package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
public class TaskChainResponse {
  boolean chainEnd;
  PassThroughData passThroughData;
  TaskRequest taskRequest;

  public static class TaskChainResponseBuilder {
    public TaskChainResponse build() {
      if (taskRequest == null && !chainEnd) {
        throw new InvalidRequestException("Task Cannot be null if not chain end");
      }
      return internalBuild();
    }
  }
}
