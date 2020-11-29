package io.harness.pms.plan.creator.plan;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.MapStepParameters;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.plan.creation.PlanCreationContext;
import io.harness.pms.plan.creation.PlanCreationResponse;
import io.harness.pms.steps.StepType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class SimpleStepPlanCreator implements PartialPlanCreator<YamlField> {
  public abstract Set<String> getSupportedStepTypes();

  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stepTypes = getSupportedStepTypes();
    if (EmptyPredicate.isEmpty(stepTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap("step", stepTypes);
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field) {
    YamlNode yamlNode = field.getNode();
    YamlNode specYamlNode = Preconditions.checkNotNull(yamlNode.getField("spec")).getNode();
    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(yamlNode.getUuid())
            .identifier(yamlNode.getIdentifier())
            .stepType(StepType.newBuilder().setType(yamlNode.getType()).build())
            .name(yamlNode.getNameOrIdentifier())
            .group(yamlNode.getType())
            .stepParameters(new MapStepParameters("spec", specYamlNode.toString()))
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder().setType("SYNC").build())
                                       .build())
            .skipExpressionChain(false)
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }
}
