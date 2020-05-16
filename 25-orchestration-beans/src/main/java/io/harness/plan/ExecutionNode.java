package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserObtainment;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.references.RefObject;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class ExecutionNode {
  // Identifiers
  @NotNull String uuid;
  @NotNull String name;
  @NotNull StateType stateType;
  @NotNull String identifier;

  // Input/Outputs
  StateParameters stateParameters;
  @Singular List<RefObject> refObjects;

  // Hooks
  @Singular List<AdviserObtainment> adviserObtainments;
  @Singular List<FacilitatorObtainment> facilitatorObtainments;
}
