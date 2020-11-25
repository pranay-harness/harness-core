package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.steps.SkipType;
import io.harness.pms.steps.StepType;
import io.harness.serializer.JsonUtils;
import io.harness.state.io.StepParameters;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.bson.Document;

@Value
@Builder
@OwnedBy(CDC)
@Redesign
@FieldNameConstants(innerTypeName = "PlanNodeKeys")
public class PlanNode {
  // Identifiers
  @NotNull String uuid;
  @NotNull String name;
  @NotNull StepType stepType;
  @NotNull String identifier;
  String group;

  // Input/Outputs
  Document stepParameters;
  @Singular List<RefObject> refObjects;

  // Hooks
  @Singular List<AdviserObtainment> adviserObtainments;
  @Singular List<FacilitatorObtainment> facilitatorObtainments;

  // skip
  boolean skipExpressionChain;

  @Builder.Default SkipType skipGraphType = SkipType.NOOP;

  public PlanNode cloneForRetry(StepParameters parameters) {
    return PlanNode.builder()
        .uuid(uuid)
        .name(name)
        .stepType(stepType)
        .identifier(identifier)
        .group(group)
        .stepParameters(parameters)
        .refObjects(refObjects)
        .adviserObtainments(adviserObtainments)
        .facilitatorObtainments(facilitatorObtainments)
        .skipExpressionChain(skipExpressionChain)
        .build();
  }

  public static class PlanNodeBuilder {
    public PlanNodeBuilder stepParameters(final StepParameters stepParameters) {
      this.stepParameters = stepParameters == null ? null : Document.parse(JsonUtils.asJson(stepParameters));
      return this;
    }
  }
}
