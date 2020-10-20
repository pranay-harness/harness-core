package io.harness.pipeline.plan.scratch.lib.creator;

import com.google.common.base.Preconditions;

import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.pipeline.plan.scratch.common.creator.PartialPlanCreator;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class SimpleStepPlanCreator implements PartialPlanCreator {
  public abstract Set<String> getSupportedStepTypes();

  @Override
  public boolean supportsField(YamlField field) {
    YamlNode yamlNode = field.getNode();
    String type = yamlNode.getType();
    return type != null && getSupportedStepTypes().contains(type);
  }

  @Override
  public PlanCreationResponse createPlanForField(YamlField field) {
    YamlNode yamlNode = field.getNode();
    YamlNode specYamlNode = Preconditions.checkNotNull(yamlNode.getField("spec")).getNode();
    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(yamlNode.getUuid())
            .identifier(yamlNode.getIdentifier())
            .stepType(StepType.builder().type(yamlNode.getType()).build())
            .name(yamlNode.getNameOrIdentifier())
            .group(yamlNode.getType())
            .stepParameters(new MapStepParameters("spec", specYamlNode.toString()))
            .facilitatorObtainment(
                FacilitatorObtainment.builder().type(FacilitatorType.builder().type("SYNC").build()).build())
            .skipExpressionChain(false)
            .build();

    Map<String, PlanNode> nodes = new HashMap<>();
    nodes.put(stepPlanNode.getUuid(), stepPlanNode);
    return PlanCreationResponse.builder().nodes(nodes).build();
  }
}
