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
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

@OwnedBy(CDC)
@Redesign
public class PlanNode {
  // Identifiers
  @NonNull @Getter String uuid;
  @NonNull @Getter String name;
  @NonNull @Getter StepType stepType;
  @NonNull @Getter String identifier;

  // Input/Outputs
  @Getter StepParameters stepParameters;
  @Singular @Getter List<RefObject> refObjects;

  // Hooks
  @Singular @Getter List<AdviserObtainment> adviserObtainments;
  @Singular @Getter List<FacilitatorObtainment> facilitatorObtainments;

  boolean skipExpressionChain;

  @Builder
  public PlanNode(@NonNull String uuid, @NonNull String name, @NonNull StepType stepType, @NonNull String identifier,
      StepParameters stepParameters, @Singular List<RefObject> refObjects,
      @Singular List<AdviserObtainment> adviserObtainments,
      @Singular List<FacilitatorObtainment> facilitatorObtainments, boolean skipExpressionChain) {
    this.uuid = uuid;
    this.name = name;
    this.stepType = stepType;
    this.identifier = identifier;
    this.stepParameters = stepParameters;
    this.refObjects = refObjects;
    this.adviserObtainments = adviserObtainments;
    this.facilitatorObtainments = facilitatorObtainments;
    this.skipExpressionChain = skipExpressionChain;
  }

  PlanNode(String uuid) {
    this.uuid = uuid;
  }
}
