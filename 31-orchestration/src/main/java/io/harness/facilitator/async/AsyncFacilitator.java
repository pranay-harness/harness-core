package io.harness.facilitator.async;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.state.io.StateTransput;

import java.util.List;

@Redesign
@Produces(Facilitator.class)
public class AsyncFacilitator implements Facilitator {
  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.ASYNC).build();
  }

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, FacilitatorParameters parameters, List<StateTransput> inputs) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.ASYNC)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
