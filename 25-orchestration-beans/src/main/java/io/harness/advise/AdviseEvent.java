package io.harness.advise;

import io.harness.annotations.Redesign;
import io.harness.exception.FailureType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;
import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class AdviseEvent {
  @NotNull StateResponse stateResponse;
  StateParameters stateParameters;
  EnumSet<FailureType> failureTypes; // This can be part of the response itself
}
