package io.harness.plan;

import io.harness.adviser.AdviserObtainment;
import io.harness.annotations.Redesign;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.refrences.RefObject;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class ExecutionNode {
  // Identifiers
  @NotNull String uuid;
  @NotNull String name;
  @NotNull String stateType;

  // Input/Outputs
  StateParameters stateParameters;
  @Singular List<RefObject> refObjects;

  // Hooks
  @Singular List<AdviserObtainment> adviserObtainments;
  @Singular List<FacilitatorObtainment> facilitatorObtainments;
}
