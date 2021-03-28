package io.harness.cdng.creator.plan.infrastructure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.creator.plan.rollback.ExecutionRollbackPMSPlanCreator;
import io.harness.cdng.creator.plan.rollback.StepGroupsRollbackPMSPlanCreator;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class InfraRollbackPMSPlanCreator {
  public static final String INFRA_ROLLBACK_NODE_ID_PREFIX = "infraRollback";

  public static PlanCreationResponse createInfraRollbackPlan(YamlField infraField) {
    if (!isDynamicallyProvisioned(infraField)) {
      return PlanCreationResponse.builder().build();
    }

    YamlField provisionerField = infraField.getNode()
                                     .getField(YamlTypes.INFRASTRUCTURE_DEF)
                                     .getNode()
                                     .getField(YAMLFieldNameConstants.PROVISIONER);

    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    YamlField stepsField = provisionerField.getNode().getField(YAMLFieldNameConstants.STEPS);
    PlanCreationResponse stepGroupsRollbackPlan =
        StepGroupsRollbackPMSPlanCreator.createStepGroupsRollbackPlanNode(stepsField);
    if (isNotEmpty(stepGroupsRollbackPlan.getNodes())) {
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(stepsField.getNode().getUuid() + PlanCreationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX)
              .shouldAlwaysRun(true)
              .build());
    }

    YamlField rollbackStepsField = provisionerField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    PlanCreationResponse executionRollbackPlan =
        ExecutionRollbackPMSPlanCreator.createExecutionRollbackPlanNode(rollbackStepsField);
    if (isNotEmpty(executionRollbackPlan.getNodes())) {
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(rollbackStepsField.getNode().getUuid() + PlanCreationConstants.ROLLBACK_STEPS_NODE_ID_PREFIX)
              .shouldAlwaysRun(true)
              .build());
    }

    if (isEmpty(stepParametersBuilder.build().getChildNodes())) {
      return PlanCreationResponse.builder().build();
    }

    PlanNode infraRollbackNode =
        PlanNode.builder()
            .uuid(infraField.getNode().getUuid() + INFRA_ROLLBACK_NODE_ID_PREFIX)
            .name(PlanCreationConstants.INFRA_ROLLBACK_NODE_NAME)
            .identifier(PlanCreationConstants.INFRA_ROLLBACK_NODE_IDENTIFIER)
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(stepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .skipExpressionChain(true)
            // TODO: change this to not skip node
            .skipGraphType(SkipType.SKIP_NODE)
            .build();

    PlanCreationResponse finalResponse =
        PlanCreationResponse.builder().node(infraRollbackNode.getUuid(), infraRollbackNode).build();
    finalResponse.merge(stepGroupsRollbackPlan);
    finalResponse.merge(executionRollbackPlan);

    return finalResponse;
  }

  private static boolean isDynamicallyProvisioned(YamlField infraField) {
    if (infraField == null) {
      return false;
    }
    YamlField infraDefField = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    if (infraDefField == null) {
      return false;
    }
    YamlField provisionerField = infraDefField.getNode().getField(YAMLFieldNameConstants.PROVISIONER);
    return provisionerField != null;
  }
}
