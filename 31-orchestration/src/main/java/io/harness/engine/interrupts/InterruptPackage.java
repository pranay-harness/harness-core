package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class InterruptPackage {
  @NonNull String planExecutionId;
  @NonNull ExecutionInterruptType interruptType;
  String nodeExecutionId;
  StepParameters parameters;
  EmbeddedUser embeddedUser;
}
