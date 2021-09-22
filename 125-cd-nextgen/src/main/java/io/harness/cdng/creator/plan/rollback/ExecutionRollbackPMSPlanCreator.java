package io.harness.cdng.creator.plan.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

@OwnedBy(HarnessTeam.CDC)
public class ExecutionRollbackPMSPlanCreator {
  public static PlanCreationResponse createExecutionRollbackPlanNode(YamlNode executionField) {
    if (executionField == null) {
      return PlanCreationResponse.builder().build();
    }
    YamlField executionStepsField = executionField.getField(YAMLFieldNameConstants.STEPS);

    if (executionStepsField == null || executionStepsField.getNode().asArray().size() == 0) {
      return PlanCreationResponse.builder().build();
    }
    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    String executionNodeFullIdentifier =
        YamlUtils.getQualifiedNameTillGivenField(executionField, YAMLFieldNameConstants.STAGES);
    YamlField executionRollbackSteps = executionField.getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    // Add rollbackSteps defined under rollback section of execution.
    PlanCreationResponse executionRollbackPlanNode =
        ExecutionStepsRollbackPMSPlanCreator.createExecutionStepsRollbackPlanNode(executionRollbackSteps);
    if (executionRollbackSteps != null && executionRollbackSteps.getNode() != null
        && EmptyPredicate.isNotEmpty(executionRollbackPlanNode.getNodes())) {
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(executionRollbackSteps.getNode().getUuid() + OrchestrationConstants.ROLLBACK_STEPS_NODE_ID_SUFFIX)
              .dependentNodeIdentifier(executionNodeFullIdentifier)
              .build());
    }

    if (EmptyPredicate.isEmpty(stepParametersBuilder.build().getChildNodes())) {
      return PlanCreationResponse.builder().build();
    }

    PlanNode deploymentStageRollbackNode =
        PlanNode.builder()
            .uuid(executionStepsField.getNode().getUuid() + OrchestrationConstants.ROLLBACK_EXECUTION_NODE_ID_SUFFIX)
            .name(OrchestrationConstants.EXECUTION_NODE_NAME + " " + OrchestrationConstants.ROLLBACK_NODE_NAME)
            .identifier(YAMLFieldNameConstants.ROLLBACK_STEPS)
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(stepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .skipExpressionChain(true)
            .build();

    PlanCreationResponse finalResponse =
        PlanCreationResponse.builder().node(deploymentStageRollbackNode.getUuid(), deploymentStageRollbackNode).build();
    finalResponse.merge(executionRollbackPlanNode);

    return finalResponse;
  }
}
