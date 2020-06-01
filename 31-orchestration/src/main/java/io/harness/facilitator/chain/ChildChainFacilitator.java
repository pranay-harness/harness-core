package io.harness.facilitator.chain;

import io.harness.ambiance.Ambiance;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;

import java.util.List;

public class ChildChainFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build();

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, FacilitatorParameters parameters, List<StepTransput> inputs) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.CHILD_CHAIN)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
