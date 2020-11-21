package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class InterruptEffect {
  @NotNull String interruptId;
  @NotNull long tookEffectAt;
  @NotNull ExecutionInterruptType interruptType;
}
