package io.harness.cdng.creator.plan.rollback;

import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters.RollbackOptionalChildrenParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ParallelStepGroupRollbackPMSPlanCreator {
  public static PlanCreationResponse createParallelStepGroupRollbackPlan(YamlField parallelStepGroup) {
    List<YamlField> stepGroupFields = getStepGroupInParallelSectionHavingRollback(parallelStepGroup);
    if (EmptyPredicate.isEmpty(stepGroupFields)) {
      return PlanCreationResponse.builder().build();
    }

    YamlNode stageNode = YamlUtils.getGivenYamlNodeFromParentPath(parallelStepGroup.getNode(), "stage");
    RollbackOptionalChildrenParametersBuilder rollbackOptionalChildrenParametersBuilder =
        RollbackOptionalChildrenParameters.builder();

    PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
    PlanCreationResponse stepGroupResponses = PlanCreationResponse.builder().build();
    for (YamlField stepGroupField : stepGroupFields) {
      YamlField rollbackStepsNode = stepGroupField.getNode().getField("rollbackSteps");
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
            .name("Parallel Step Groups Rollback")
            .identifier(PlanCreatorConstants.PARALLEL_STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER)
            .stepType(RollbackOptionalChildrenStep.STEP_TYPE)
            .stepParameters(rollbackOptionalChildrenParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .build();

    PlanCreationResponse finalResponse =
        planCreationResponseBuilder.node(parallelStepGroupsRollbackNode.getUuid(), parallelStepGroupsRollbackNode)
            .build();
    finalResponse.merge(stepGroupResponses);
    return finalResponse;
  }

  private static List<YamlField> getStepGroupInParallelSectionHavingRollback(YamlField parallelStepGroup) {
    List<YamlNode> yamlNodes =
        Optional.of(Preconditions.checkNotNull(parallelStepGroup).getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stepGroupFields = new LinkedList<>();
    yamlNodes.forEach(yamlNode -> {
      YamlField stepGroupField = yamlNode.getField("stepGroup");
      if (stepGroupField != null && stepGroupField.getNode().getField("rollbackSteps") != null) {
        stepGroupFields.add(stepGroupField);
      }
    });
    return stepGroupFields;
  }
}
