package io.harness.state.io;

import io.harness.annotations.Redesign;
import io.harness.exception.FailureType;
import io.harness.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.EnumSet;
import java.util.List;

@Value
@Builder
@Redesign
public class StateResponse {
  @NonNull NodeExecutionStatus status;
  @Singular List<StateTransput> outputs;
  FailureInfo failureInfo;

  @Value
  @Builder
  public static class FailureInfo {
    String errorMessage;
    @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
  }
}
