package io.harness.pms.sdk.core.plan;

import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.steps.SkipType;
import io.harness.pms.steps.StepType;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
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

  // Skip
  boolean skipExpressionChain;
  @Builder.Default SkipType skipGraphType = SkipType.NOOP;
}
