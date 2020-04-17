package io.harness.redesign.states.wait;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class WaitStateParameters implements StateParameters {
  long waitDurationSeconds;
}
