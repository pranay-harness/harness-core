package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("interruptEffect")
public class InterruptEffect {
  @NotNull String interruptId;
  @NotNull long tookEffectAt;
  @NotNull InterruptType interruptType;
  @NotNull InterruptConfig interruptConfig;
}
