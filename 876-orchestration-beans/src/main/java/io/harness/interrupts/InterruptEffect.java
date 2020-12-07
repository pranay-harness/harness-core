package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@TypeAlias("interruptEffect")
public class InterruptEffect {
  @NotNull String interruptId;
  @NotNull long tookEffectAt;
  @NotNull ExecutionInterruptType interruptType;
}
