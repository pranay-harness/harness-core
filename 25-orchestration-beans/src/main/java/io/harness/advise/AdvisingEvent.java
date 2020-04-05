package io.harness.advise;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class AdvisingEvent {
  @NotNull StateResponse stateResponse;
  StateParameters stateParameters;
}
