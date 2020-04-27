package io.harness.facilitator.children;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class ChildrenFacilitator implements Facilitator {
  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.CHILDREN).build();
  }

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, List<StateTransput> inputs) {
    return FacilitatorResponse.builder().executionMode(ExecutionMode.CHILDREN).build();
  }
}
