package io.harness.pms.sdk.core.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.timeout.SdkTimeoutObtainment;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(HarnessTeam.PIPELINE)
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
  String stepInputs;
  @Singular List<RefObject> refObjects;

  // Hooks
  @Singular List<AdviserObtainment> adviserObtainments;
  @Singular List<FacilitatorObtainment> facilitatorObtainments;
  @Singular List<SdkTimeoutObtainment> timeoutObtainments;

  // Skip
  String skipCondition;
  String whenCondition;

  // stage fqn
  @NonFinal @lombok.Setter String stageFqn;

  // Config
  boolean skipExpressionChain;
  @Builder.Default SkipType skipGraphType = SkipType.NOOP;
  boolean skipUnresolvedExpressionsCheck;
}
