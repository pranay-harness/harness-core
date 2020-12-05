package io.harness.plancreator.stages;

import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import java.util.*;

public class StagesPlanCreator extends ChildrenPlanCreator<StagesConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, StagesConfig config) {
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    List<YamlField> stageYamlFields = getStageYamlFields(ctx);
    for (YamlField stageYamlField : stageYamlFields) {
      Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
      stageYamlFieldMap.put(stageYamlField.getNode().getUuid(), stageYamlField);
      responseMap.put(
          stageYamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(stageYamlFieldMap).build());
    }
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, StagesConfig config, List<String> childrenNodeIds) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childrenNodeIds.get(0)).logMessage("Stages").build();
    return PlanNode.builder()
        .uuid(ctx.getCurrentField().getNode().getUuid())
        .identifier(PlanCreationConstants.STAGES_NODE_IDENTIFIER)
        .stepType(NGSectionStep.STEP_TYPE)
        .group(StepOutcomeGroup.STAGES.name())
        .name(PlanCreationConstants.STAGES_NODE_IDENTIFIER)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<StagesConfig> getFieldClass() {
    return StagesConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stages", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<YamlField> getStageYamlFields(PlanCreationContext planCreationContext) {
    List<YamlNode> yamlNodes =
        Optional.of(planCreationContext.getCurrentField().getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stageFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stageField = yamlNode.getField("stage");
      YamlField parallelStageField = yamlNode.getField("parallel");
      if (stageField != null) {
        stageFields.add(stageField);
      } else if (parallelStageField != null) {
        stageFields.add(parallelStageField);
      }
    });
    return stageFields;
  }
}
