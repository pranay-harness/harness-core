package io.harness.interrupts;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class InterruptEffect {
  @NotNull String interruptId;
  @NotNull Date tookEffectAt;
}
