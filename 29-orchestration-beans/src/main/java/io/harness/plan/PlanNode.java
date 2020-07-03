package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserObtainment;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.references.RefObject;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
@OwnedBy(CDC)
@Redesign
public class PlanNode {
  // Identifiers
  @NotNull String uuid;
  @NotNull String name;
  @NotNull StepType stepType;
  @NotNull String identifier;
  String group;

  // Input/Outputs
  StepParameters stepParameters;
  @Singular List<RefObject> refObjects;

  // Hooks
  @Singular List<AdviserObtainment> adviserObtainments;
  @Singular List<FacilitatorObtainment> facilitatorObtainments;

  boolean skipExpressionChain;
}
