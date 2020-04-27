package io.harness.facilitator.child;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.state.io.StateTransput;
import lombok.Builder;

import java.util.List;

@Redesign
@Builder
public class ChildFacilitator implements Facilitator {
  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.CHILD).build();
  }

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, List<StateTransput> inputs) {
    return FacilitatorResponse.builder().executionMode(ExecutionMode.CHILD).build();
  }
}
