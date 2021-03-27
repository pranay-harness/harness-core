package io.harness.cdng.creator.plan.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.infrastructure.InfraRollbackPMSPlanCreator;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class RollbackPMSPlanCreator extends ChildrenPlanCreator<YamlField> {
  HashMap<String, Boolean> planCreationChecker = new HashMap<>();

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, YamlField config) {
    YamlNode parentNode = YamlUtils.findParentNode(config.getNode(), YAMLFieldNameConstants.EXECUTION);
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();

    YamlField executionStepsField = parentNode.getField(YAMLFieldNameConstants.STEPS);

    if (executionStepsField == null || executionStepsField.getNode().asArray().size() == 0) {
      return responseMap;
    }

    // stepGroupRollback
    PlanCreationResponse stepGroupsRollbackPlanNode =
        StepGroupsRollbackPMSPlanCreator.createStepGroupsRollbackPlanNode(executionStepsField);
    if (EmptyPredicate.isNotEmpty(stepGroupsRollbackPlanNode.getNodes())) {
      String stepGroupRollbackUuid =
          executionStepsField.getNode().getUuid() + PlanCreationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX;
      planCreationChecker.put(stepGroupRollbackUuid, true);
      responseMap.put(stepGroupRollbackUuid, stepGroupsRollbackPlanNode);
    }

    // executionRollback
    YamlField executionRollbackSteps = parentNode.getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    PlanCreationResponse executionRollbackPlanNode =
        ExecutionRollbackPMSPlanCreator.createExecutionRollbackPlanNode(executionRollbackSteps);
    if (EmptyPredicate.isNotEmpty(executionRollbackPlanNode.getNodes())) {
      String executionRollbackUuid =
          executionRollbackSteps.getNode().getUuid() + PlanCreationConstants.ROLLBACK_STEPS_NODE_ID_PREFIX;
      planCreationChecker.put(executionRollbackUuid, true);
      responseMap.put(executionRollbackUuid, executionRollbackPlanNode);
    }

    YamlField infraField = parentNode.nextSiblingNodeFromParentObject(YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE);
    PlanCreationResponse infraRollbackPlanNode = InfraRollbackPMSPlanCreator.createInfraRollbackPlan(infraField);
    if (EmptyPredicate.isNotEmpty(infraRollbackPlanNode.getNodes())) {
      String infraRollbackUuid = infraField.getNode().getUuid() + "infraRollback";
      planCreationChecker.put(infraRollbackUuid, true);
      responseMap.put(infraRollbackUuid, infraRollbackPlanNode);
    }

    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, YamlField config, List<String> childrenNodeIds) {
    YamlNode parentNode = YamlUtils.findParentNode(config.getNode(), YAMLFieldNameConstants.EXECUTION);
    YamlField executionStepsField = parentNode.getField(YAMLFieldNameConstants.STEPS);

    YamlNode stageNode = YamlUtils.getGivenYamlNodeFromParentPath(config.getNode(), YAMLFieldNameConstants.STAGE);

    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    String executionNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
        stageNode.getIdentifier(), PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER);

    if (executionStepsField != null && executionStepsField.getNode() != null) {
      String stepGroupRollbackUuid =
          executionStepsField.getNode().getUuid() + PlanCreationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX;

      if (planCreationChecker.containsKey(stepGroupRollbackUuid)) {
        stepParametersBuilder.childNode(RollbackNode.builder()
                                            .nodeId(stepGroupRollbackUuid)
                                            .dependentNodeIdentifier(executionNodeFullIdentifier)
                                            .build());
      }
    }

    YamlField executionRollbackSteps = parentNode.getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    if (executionRollbackSteps != null && executionRollbackSteps.getNode() != null) {
      String executionRollbackStepsUuid =
          executionRollbackSteps.getNode().getUuid() + PlanCreationConstants.ROLLBACK_STEPS_NODE_ID_PREFIX;
      if (planCreationChecker.containsKey(executionRollbackStepsUuid)) {
        stepParametersBuilder.childNode(RollbackNode.builder()
                                            .nodeId(executionRollbackStepsUuid)
                                            .dependentNodeIdentifier(executionNodeFullIdentifier)
                                            .build());
      }
    }

    YamlField infraRollbackSteps = parentNode.getField(YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE);
    if (infraRollbackSteps != null && infraRollbackSteps.getNode() != null) {
      String infraRollbackUuid = infraRollbackSteps.getNode().getUuid() + "infraRollback";
      if (planCreationChecker.containsKey(infraRollbackUuid)) {
        stepParametersBuilder.childNode(RollbackNode.builder()
                                            .nodeId(infraRollbackUuid)
                                            .dependentNodeIdentifier(executionNodeFullIdentifier)
                                            .build());
      }
    }

    return PlanNode.builder()
        .uuid(config.getNode().getUuid())
        .name(PlanCreationConstants.ROLLBACK_NODE_NAME)
        .identifier(YAMLFieldNameConstants.ROLLBACK_STEPS)
        .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
        .stepParameters(stepParametersBuilder.build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                .build())
        .skipExpressionChain(true)
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }

  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.ROLLBACK_STEPS, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
