package io.harness.facilitator.children;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;

import java.util.List;

@OwnedBy(CDC)
@Redesign
@Produces(Facilitator.class)
public class ChildrenFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.builder().type(FacilitatorType.CHILDREN).build();

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, FacilitatorParameters parameters, List<StepTransput> inputs) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.CHILDREN)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
