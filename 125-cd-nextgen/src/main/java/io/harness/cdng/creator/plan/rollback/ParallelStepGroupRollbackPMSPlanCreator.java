package io.harness.cdng.creator.plan.rollback;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters.RollbackOptionalChildrenParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.List;

public class ParallelStepGroupRollbackPMSPlanCreator {
  public static PlanCreationResponse createParallelStepGroupRollbackPlan(YamlField parallelStepGroup) {
    List<YamlField> stepGroupFields = PlanCreatorUtils.getStepGroupInParallelSectionHavingRollback(parallelStepGroup);
    if (hasNone(stepGroupFields)) {
      return PlanCreationResponse.builder().build();
    }

    YamlNode stageNode =
        YamlUtils.getGivenYamlNodeFromParentPath(parallelStepGroup.getNode(), YAMLFieldNameConstants.STAGE);
    RollbackOptionalChildrenParametersBuilder rollbackOptionalChildrenParametersBuilder =
        RollbackOptionalChildrenParameters.builder();

    PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
    PlanCreationResponse stepGroupResponses = PlanCreationResponse.builder().build();
    for (YamlField stepGroupField : stepGroupFields) {
      YamlField rollbackStepsNode = stepGroupField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
      RollbackNode rollbackNode =
          RollbackNode.builder()
              .nodeId(rollbackStepsNode.getNode().getUuid())
              .dependentNodeIdentifier(PlanCreatorConstants.STAGES_NODE_IDENTIFIER + "." + stageNode.getIdentifier()
                  + "." + PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER + "."
                  + stepGroupField.getNode().getIdentifier())
              .build();
      rollbackOptionalChildrenParametersBuilder.parallelNode(rollbackNode);
      PlanCreationResponse stepGroupRollbackPlan =
          StepGroupRollbackPMSPlanCreator.createStepGroupRollbackPlan(stepGroupField);
      stepGroupResponses.merge(stepGroupRollbackPlan);
    }

    PlanNode parallelStepGroupsRollbackNode =
        PlanNode.builder()
            .uuid(parallelStepGroup.getNode().getUuid() + "_rollback")
            .name("Parallel StepGroups Rollback")
            .identifier(PlanCreatorConstants.PARALLEL_STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER
                + parallelStepGroup.getNode().getUuid() + "_rollback")
            .stepType(RollbackOptionalChildrenStep.STEP_TYPE)
            .stepParameters(rollbackOptionalChildrenParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .skipExpressionChain(true)
            .build();

    PlanCreationResponse finalResponse =
        planCreationResponseBuilder.node(parallelStepGroupsRollbackNode.getUuid(), parallelStepGroupsRollbackNode)
            .build();
    finalResponse.merge(stepGroupResponses);
    return finalResponse;
  }
}
