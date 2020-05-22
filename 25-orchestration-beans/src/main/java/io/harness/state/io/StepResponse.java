package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import io.harness.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.EnumSet;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
@Redesign
public class StepResponse {
  @NonNull NodeExecutionStatus status;
  @Singular Map<String, StepTransput> outcomes;
  FailureInfo failureInfo;

  @Value
  @Builder
  public static class FailureInfo {
    String errorMessage;
    @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
  }
}
