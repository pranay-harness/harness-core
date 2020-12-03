package io.harness.plancreator.stages.parallel;

import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.steps.StepType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.fork.ForkStepParameters;

import com.google.common.annotations.VisibleForTesting;
import java.util.*;
import java.util.stream.Collectors;

public class ParallelPlanCreator implements PartialPlanCreator<YamlField> {
  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("parallel", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field) {
    YamlNode currentNode = field.getNode();
    Map<String, YamlField> dependencies = getDependencyNodeIds(ctx);
    PlanNode planNode =
        PlanNode.builder()
            .uuid(currentNode.getUuid())
            .name("parallel")
            .identifier("parallel" + currentNode.getUuid())
            .stepType(StepType.newBuilder().setType(ForkStep.STEP_TYPE.getType()).build())
            .group(StepOutcomeGroup.STEP.name())
            .stepParameters(ForkStepParameters.builder().parallelNodeIds(dependencies.keySet()).build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .skipExpressionChain(true)
            .build();
    return PlanCreationResponse.builder().dependencies(dependencies).node(currentNode.getUuid(), planNode).build();
  }

  @VisibleForTesting
  Map<String, YamlField> getDependencyNodeIds(PlanCreationContext planCreationContext) {
    Map<String, YamlField> childYamlFields = getChildYamlFieldsForGiveType(planCreationContext, "stage");

    if (childYamlFields.isEmpty()) {
      childYamlFields.putAll(getChildYamlFieldsForGiveType(planCreationContext, "step"));
    }

    if (childYamlFields.isEmpty()) {
      childYamlFields.putAll(getChildYamlFieldsForGiveType(planCreationContext, "stepGroup"));
    }
    return childYamlFields;
  }

  private Map<String, YamlField> getChildYamlFieldsForGiveType(PlanCreationContext planCreationContext, String type) {
    return Optional.of(planCreationContext.getCurrentField().getNode().asArray())
        .orElse(Collections.emptyList())
        .stream()
        .map(el -> el.getField(type))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(field -> field.getNode().getUuid(), field -> field));
  }
}
