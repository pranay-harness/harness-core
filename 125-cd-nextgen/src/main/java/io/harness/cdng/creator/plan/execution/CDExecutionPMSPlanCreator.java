package io.harness.cdng.creator.plan.execution;

import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.rollback.RollbackPlanCreator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.rollback.RollbackCustomAdviser;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CDExecutionPMSPlanCreator extends ChildrenPlanCreator<ExecutionElementConfig> {
  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ExecutionElementConfig config) {
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    List<YamlField> stepYamlFields = getStepYamlFields(ctx);
    for (YamlField stepYamlField : stepYamlFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      responseMap.put(
          stepYamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(stepYamlFieldMap).build());
    }
    // Add Steps Node
    if (EmptyPredicate.isNotEmpty(stepYamlFields)) {
      YamlField stepsField =
          Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
      PlanNode stepsNode = getStepsPlanNode(stepsField, stepYamlFields.get(0).getNode().getUuid());
      responseMap.put(stepsNode.getUuid(), PlanCreationResponse.builder().node(stepsNode.getUuid(), stepsNode).build());
    }

    YamlField executionStepsField = ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS);

    PlanCreationResponse planForRollback = RollbackPlanCreator.createPlanForRollback(ctx.getCurrentField());
    if (EmptyPredicate.isNotEmpty(planForRollback.getNodes())) {
      responseMap.put(
          Objects.requireNonNull(executionStepsField).getNode().getUuid() + "_combinedRollback", planForRollback);
    }
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ExecutionElementConfig config, List<String> childrenNodeIds) {
    YamlField stepsField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
    StepParameters stepParameters = NGSectionStepParameters.builder()
                                        .childNodeId(stepsField.getNode().getUuid())
                                        .logMessage("Execution Element")
                                        .build();
    return PlanNode.builder()
        .uuid(ctx.getCurrentField().getNode().getUuid())
        .identifier(PlanCreationConstants.EXECUTION_NODE_IDENTIFIER)
        .stepType(NGSectionStep.STEP_TYPE)
        .group(StepOutcomeGroup.EXECUTION.name())
        .name(PlanCreationConstants.EXECUTION_NODE_NAME)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainment(AdviserObtainment.newBuilder().setType(RollbackCustomAdviser.ADVISER_TYPE).build())
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<ExecutionElementConfig> getFieldClass() {
    return ExecutionElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.EXECUTION, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<YamlField> getStepYamlFields(PlanCreationContext planCreationContext) {
    List<YamlNode> yamlNodes = Optional
                                   .of(Preconditions
                                           .checkNotNull(planCreationContext.getCurrentField().getNode().getField(
                                               YAMLFieldNameConstants.STEPS))
                                           .getNode()
                                           .asArray())
                                   .orElse(Collections.emptyList());

    return InfrastructurePmsPlanCreator.getStepYamlFields(yamlNodes);
  }

  PlanNode getStepsPlanNode(YamlField stepsYamlField, String childNodeId) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childNodeId).logMessage("Steps Element").build();
    return PlanNode.builder()
        .uuid(stepsYamlField.getNode().getUuid())
        .identifier(YAMLFieldNameConstants.STEPS)
        .stepType(NGSectionStep.STEP_TYPE)
        .name(YAMLFieldNameConstants.STEPS)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
