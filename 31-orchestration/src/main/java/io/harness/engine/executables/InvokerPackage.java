package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StepInputPackage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class InvokerPackage {
  @NonNull NodeExecution nodeExecution;
  StepInputPackage inputPackage;
  PassThroughData passThroughData;
}
