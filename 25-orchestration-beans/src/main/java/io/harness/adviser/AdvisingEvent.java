package io.harness.adviser;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.state.io.StateResponse;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@Redesign
public class AdvisingEvent {
  @NonNull Ambiance ambiance;
  StateResponse stateResponse;
  AdviserParameters adviserParameters;
}
