/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters.RollbackOptionalChildrenParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.List;

@OwnedBy(CDC)
public class ParallelStepGroupRollbackPMSPlanCreator {
  public static PlanCreationResponse createParallelStepGroupRollbackPlan(YamlField parallelStepGroup) {
    List<YamlField> stepGroupFields = PlanCreatorUtils.getStepGroupInParallelSectionHavingRollback(parallelStepGroup);
    if (EmptyPredicate.isEmpty(stepGroupFields)) {
      return PlanCreationResponse.builder().build();
    }

    RollbackOptionalChildrenParametersBuilder rollbackOptionalChildrenParametersBuilder =
        RollbackOptionalChildrenParameters.builder();

    PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
    PlanCreationResponse stepGroupResponses = PlanCreationResponse.builder().build();
    for (YamlField stepGroupField : stepGroupFields) {
      YamlField rollbackStepsNode = stepGroupField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
      RollbackNode rollbackNode = RollbackNode.builder()
                                      .nodeId(rollbackStepsNode.getNode().getUuid())
                                      .dependentNodeIdentifier(YamlUtils.getQualifiedNameTillGivenField(
                                          stepGroupField.getNode(), YAMLFieldNameConstants.STAGES))
                                      .build();
      rollbackOptionalChildrenParametersBuilder.parallelNode(rollbackNode);
      PlanCreationResponse stepGroupRollbackPlan =
          StepGroupRollbackPMSPlanCreator.createStepGroupRollbackPlan(stepGroupField);
      stepGroupResponses.merge(stepGroupRollbackPlan);
    }

    PlanNode parallelStepGroupsRollbackNode =
        PlanNode.builder()
            .uuid(parallelStepGroup.getNode().getUuid() + "_rollback")
            .name(PlanCreatorConstants.PARALLEL_STEP_GROUPS_ROLLBACK_NODE_NAME)
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
