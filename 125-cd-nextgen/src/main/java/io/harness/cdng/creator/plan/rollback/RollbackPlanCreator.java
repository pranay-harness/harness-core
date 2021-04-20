package io.harness.cdng.creator.plan.rollback;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.infrastructure.InfraRollbackPMSPlanCreator;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.plancreator.beans.OrchestrationConstants;
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
@OwnedBy(HarnessTeam.CDC)
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

    // Infra rollback
    YamlField infraField = executionField.getNode().nextSiblingNodeFromParentObject(YamlTypes.PIPELINE_INFRASTRUCTURE);
    PlanCreationResponse infraRollbackPlan = InfraRollbackPMSPlanCreator.createInfraRollbackPlan(infraField);
    if (isNotEmpty(infraRollbackPlan.getNodes())) {
      String infraNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
          stageNode.getIdentifier(), PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER);
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(infraField.getNode().getUuid() + InfraRollbackPMSPlanCreator.INFRA_ROLLBACK_NODE_ID_SUFFIX)
              .dependentNodeIdentifier(infraNodeFullIdentifier)
              .build());
    }

    // ExecutionRollback
    PlanCreationResponse executionRollbackPlanNode =
        ExecutionRollbackPMSPlanCreator.createExecutionRollbackPlanNode(executionField.getNode());
    if (EmptyPredicate.isNotEmpty(executionRollbackPlanNode.getNodes())) {
      String executionRollbackUuid =
          executionStepsField.getNode().getUuid() + OrchestrationConstants.ROLLBACK_EXECUTION_NODE_ID_SUFFIX;
      String executionNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
          stageNode.getIdentifier(), PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER);
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(executionRollbackUuid)
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
    }

    PlanNode deploymentStageRollbackNode =
        PlanNode.builder()
            .uuid(stageNode.getUuid() + OrchestrationConstants.COMBINED_ROLLBACK_ID_SUFFIX)
            .name(OrchestrationConstants.ROLLBACK_NODE_NAME)
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
    finalResponse.merge(executionRollbackPlanNode);
    finalResponse.merge(infraRollbackPlan);

    return finalResponse;
  }
}
