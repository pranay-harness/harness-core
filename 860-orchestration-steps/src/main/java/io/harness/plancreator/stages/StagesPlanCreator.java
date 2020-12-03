package io.harness.plancreator.stages;

import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.steps.StepType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.stages.StagesSetupStepParameters;
import io.harness.steps.section.chain.SectionChainStep;

import java.util.*;
import java.util.stream.Collectors;

public class StagesPlanCreator extends ChildrenPlanCreator<StagesConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, StagesConfig field) {
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
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, StagesConfig field, Set<String> childrenNodeIds) {
    StepParameters stepParameters = StagesSetupStepParameters.getStepParameters(field, childrenNodeIds);
    return PlanNode.builder()
        .uuid(ctx.getCurrentField().getNode().getUuid())
        .identifier(PlanCreationConstants.STAGES_NODE_IDENTIFIER)
        .stepType(StepType.newBuilder().setType(SectionChainStep.STEP_TYPE.getType()).build())
        .name(PlanCreationConstants.STAGES_NODE_IDENTIFIER)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                   .setType(FacilitatorType.newBuilder().setType("CHILD_CHAIN").build())
                                   .build())
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
    List<YamlField> stageYamlFields = Optional.of(planCreationContext.getCurrentField().getNode().asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .filter(Objects::nonNull)
                                          .collect(Collectors.toList());
    stageYamlFields.addAll(Optional.of(planCreationContext.getCurrentField().getNode().asArray())
                               .orElse(Collections.emptyList())
                               .stream()
                               .map(el -> el.getField("parallel"))
                               .filter(Objects::nonNull)
                               .collect(Collectors.toList()));
    return stageYamlFields;
  }
}
