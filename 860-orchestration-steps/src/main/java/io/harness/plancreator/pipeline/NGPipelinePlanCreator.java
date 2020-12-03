package io.harness.plancreator.pipeline;

import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.MapStepParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.common.pipeline.PipelineSetupStepParameters;

import com.google.common.base.Preconditions;
import java.util.*;

public class NGPipelinePlanCreator extends ChildrenPlanCreator<PipelineInfoConfig> {
  @Override
  public String getStartingNodeId(PipelineInfoConfig field) {
    return field.getUuid();
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, PipelineInfoConfig field) {
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    Map<String, YamlField> dependencies = new HashMap<>();
    YamlField stagesYamlNode = Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField("stages"));
    if (stagesYamlNode.getNode() == null) {
      return responseMap;
    }

    dependencies.put(stagesYamlNode.getNode().getUuid(), stagesYamlNode);
    responseMap.put(
        stagesYamlNode.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependencies).build());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, PipelineInfoConfig field, Set<String> childrenNodeIds) {
    String name = field.getName() != null ? field.getName() : field.getIdentifier();
    YamlNode stagesYamlNode = Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField("stages")).getNode();

    StepParameters stepParameters = PipelineSetupStepParameters.getStepParameters(field, stagesYamlNode.getUuid());

    return PlanNode.builder()
        .uuid(field.getUuid())
        .identifier(field.getIdentifier())
        .stepType(PipelineSetupStep.STEP_TYPE)
        .group(StepOutcomeGroup.PIPELINE.name())
        .name(name)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder().setType(FacilitatorType.newBuilder().setType("CHILD").build()).build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<PipelineInfoConfig> getFieldClass() {
    return PipelineInfoConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
