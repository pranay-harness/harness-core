package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class InterruptPackage {
  @NonNull String planExecutionId;
  @NonNull InterruptType interruptType;
  @NotNull InterruptConfig interruptConfig;
  String nodeExecutionId;
  StepParameters parameters;
  Map<String, String> metadata;
}
