package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserObtainment;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.references.RefObject;
import io.harness.state.StateType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

@OwnedBy(CDC)
@Redesign
public class ExecutionNode {
  // Identifiers
  @NonNull @Getter String uuid;
  @NonNull @Getter String name;
  @NonNull @Getter StateType stateType;
  @NonNull @Getter String identifier;

  // Input/Outputs
  @Getter StepParameters stepParameters;
  @Singular @Getter List<RefObject> refObjects;

  // Hooks
  @Singular @Getter List<AdviserObtainment> adviserObtainments;
  @Singular @Getter List<FacilitatorObtainment> facilitatorObtainments;

  @Builder
  public ExecutionNode(@NonNull String uuid, @NonNull String name, @NonNull StateType stateType,
      @NonNull String identifier, StepParameters stepParameters, @Singular List<RefObject> refObjects,
      @Singular List<AdviserObtainment> adviserObtainments,
      @Singular List<FacilitatorObtainment> facilitatorObtainments) {
    this.uuid = uuid;
    this.name = name;
    this.stateType = stateType;
    this.identifier = identifier;
    this.stepParameters = stepParameters;
    this.refObjects = refObjects;
    this.adviserObtainments = adviserObtainments;
    this.facilitatorObtainments = facilitatorObtainments;
  }

  ExecutionNode(String uuid) {
    this.uuid = uuid;
  }
}
