/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public class StepGroupsRollbackPMSPlanCreator {
  public static PlanCreationResponse createStepGroupsRollbackPlanNode(YamlField executionStepsField) {
    if (executionStepsField == null) {
      return PlanCreationResponse.builder().build();
    }
    List<YamlNode> stepsArrayFields = executionStepsField.getNode().asArray();

    RollbackOptionalChildChainStepParametersBuilder sectionOptionalChildChainStepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    PlanCreationResponse stepGroupResponses = PlanCreationResponse.builder().build();
    for (int i = stepsArrayFields.size() - 1; i >= 0; i--) {
      List<YamlField> yamlFields = stepsArrayFields.get(i).fields();
      for (YamlField yamlField : yamlFields) {
        if (yamlField.getName().equals(YAMLFieldNameConstants.STEP_GROUP)) {
          PlanCreationResponse stepGroupRollbackPlan =
              StepGroupRollbackPMSPlanCreator.createStepGroupRollbackPlan(yamlField);
          stepGroupResponses.merge(stepGroupRollbackPlan);
          if (EmptyPredicate.isNotEmpty(stepGroupRollbackPlan.getNodes())) {
            YamlField rollbackStepsNode = yamlField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
            RollbackNode rollbackNode = RollbackNode.builder()
                                            .nodeId(rollbackStepsNode.getNode().getUuid())
                                            .dependentNodeIdentifier(YamlUtils.getQualifiedNameTillGivenField(
                                                yamlField.getNode(), YAMLFieldNameConstants.STAGES))
                                            .build();
            sectionOptionalChildChainStepParametersBuilder.childNode(rollbackNode);
          }
        } else if (yamlField.getName().equals(YAMLFieldNameConstants.PARALLEL)) {
          PlanCreationResponse parallelStepGroupRollbackPlan =
              ParallelStepGroupRollbackPMSPlanCreator.createParallelStepGroupRollbackPlan(yamlField);
          stepGroupResponses.merge(parallelStepGroupRollbackPlan);
          if (EmptyPredicate.isNotEmpty(parallelStepGroupRollbackPlan.getNodes())) {
            RollbackNode rollbackNode = RollbackNode.builder()
                                            .nodeId(yamlField.getNode().getUuid() + "_rollback")
                                            .shouldAlwaysRun(true)
                                            .build();
            sectionOptionalChildChainStepParametersBuilder.childNode(rollbackNode);
          }
        }
      }
    }

    RollbackOptionalChildChainStepParameters childChainStepParameters =
        sectionOptionalChildChainStepParametersBuilder.build();
    if (EmptyPredicate.isNotEmpty(childChainStepParameters.getChildNodes())) {
      PlanNode stepGroupsRollbackNode =
          PlanNode.builder()
              .uuid(
                  executionStepsField.getNode().getUuid() + OrchestrationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_SUFFIX)
              .name(OrchestrationConstants.STEP_GROUPS_ROLLBACK_NODE_NAME)
              .identifier(PlanCreatorConstants.STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER)
              .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
              .stepParameters(childChainStepParameters)
              .facilitatorObtainment(
                  FacilitatorObtainment.newBuilder()
                      .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                      .build())
              .skipGraphType(SkipType.SKIP_NODE)
              .skipExpressionChain(true)
              .build();
      PlanCreationResponse finalResponse =
          PlanCreationResponse.builder().node(stepGroupsRollbackNode.getUuid(), stepGroupsRollbackNode).build();
      finalResponse.merge(stepGroupResponses);
      return finalResponse;
    }

    return PlanCreationResponse.builder().build();
  }
}
