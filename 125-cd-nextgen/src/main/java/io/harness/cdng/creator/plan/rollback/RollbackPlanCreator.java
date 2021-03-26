package io.harness.cdng.creator.plan.rollback;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.creator.plan.infrastructure.InfraRollbackPMSPlanCreator;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RollbackPlanCreator {
  public PlanCreationResponse createPlanForRollback(YamlField executionField) {
    YamlField executionStepsField = executionField.getNode().getField(YAMLFieldNameConstants.STEPS);

    if (executionStepsField == null || executionStepsField.getNode().asArray().size() == 0) {
      return PlanCreationResponse.builder().build();
    }
    YamlNode stageNode =
        YamlUtils.getGivenYamlNodeFromParentPath(executionField.getNode(), YAMLFieldNameConstants.STAGE);
    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    YamlField infraField = executionField.getNode().nextSiblingNodeFromParentObject(YamlTypes.PIPELINE_INFRASTRUCTURE);
    PlanCreationResponse infraRollbackPlan = InfraRollbackPMSPlanCreator.createInfraRollbackPlan(infraField);
    if (isNotEmpty(infraRollbackPlan.getNodes())) {
      String infraNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
          stageNode.getIdentifier(), PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER);
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(infraField.getNode().getUuid() + "infraRollback")
                                          .dependentNodeIdentifier(infraNodeFullIdentifier)
                                          .build());
    }

    PlanCreationResponse stepGroupsRollbackPlanNode =
        StepGroupsRollbackPMSPlanCreator.createStepGroupsRollbackPlanNode(executionStepsField);

    String executionNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
        stageNode.getIdentifier(), PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER);
    if (isNotEmpty(stepGroupsRollbackPlanNode.getNodes())) {
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(executionStepsField.getNode().getUuid()
                                              + PlanCreationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX)
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
    }
    YamlField executionRollbackSteps = executionField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    PlanCreationResponse executionRollbackPlanNode =
        ExecutionRollbackPMSPlanCreator.createExecutionRollbackPlanNode(executionRollbackSteps);
    if (isNotEmpty(executionRollbackPlanNode.getNodes())) {
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(executionRollbackSteps.getNode().getUuid() + PlanCreationConstants.ROLLBACK_STEPS_NODE_ID_PREFIX)
              .dependentNodeIdentifier(executionNodeFullIdentifier)
              .build());
    }

    if (EmptyPredicate.isEmpty(stepParametersBuilder.build().getChildNodes())) {
      return PlanCreationResponse.builder().build();
    }

    PlanNode deploymentStageRollbackNode =
        PlanNode.builder()
            .uuid(executionStepsField.getNode().getUuid() + "_combinedRollback")
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

    PlanCreationResponse finalResponse =
        PlanCreationResponse.builder().node(deploymentStageRollbackNode.getUuid(), deploymentStageRollbackNode).build();
    finalResponse.merge(stepGroupsRollbackPlanNode);
    finalResponse.merge(executionRollbackPlanNode);
    finalResponse.merge(infraRollbackPlan);

    return finalResponse;
  }
}
