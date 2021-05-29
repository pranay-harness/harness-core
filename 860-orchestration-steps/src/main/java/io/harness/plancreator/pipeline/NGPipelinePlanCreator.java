package io.harness.plancreator.pipeline;

import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.common.pipeline.PipelineSetupStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NGPipelinePlanCreator extends ChildrenPlanCreator<PipelineInfoConfig> {
  @Override
  public String getStartingNodeId(PipelineInfoConfig field) {
    return field.getUuid();
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, PipelineInfoConfig config) {
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
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
      PlanCreationContext ctx, PipelineInfoConfig config, List<String> childrenNodeIds) {
    String name = config.getName() != null ? config.getName() : config.getIdentifier();
    YamlNode stagesYamlNode = Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField("stages")).getNode();

    StepParameters stepParameters =
        PipelineSetupStepParameters.getStepParameters(ctx, config, stagesYamlNode.getUuid());

    return PlanNode.builder()
        .uuid(config.getUuid())
        .identifier(YAMLFieldNameConstants.PIPELINE)
        .stepType(PipelineSetupStep.STEP_TYPE)
        .group(StepOutcomeGroup.PIPELINE.name())
        .name(name)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<PipelineInfoConfig> getFieldClass() {
    return PipelineInfoConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.PIPELINE, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
