/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.rollback.steps.StepGroupRollbackStep;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OwnedBy(HarnessTeam.CDC)
public class StepGroupRollbackPMSPlanCreator {
  public static PlanCreationResponse createStepGroupRollbackPlan(YamlField stepGroup) {
    YamlField rollbackStepsNode = stepGroup.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    if (rollbackStepsNode != null && rollbackStepsNode.getNode().asArray().size() != 0) {
      PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
      List<YamlField> stepYamlFields = getStepYamlFields(rollbackStepsNode);
      for (YamlField stepYamlField : stepYamlFields) {
        Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
        stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
        planCreationResponseBuilder.dependencies(DependenciesUtils.toDependenciesProto(stepYamlFieldMap));
      }

      StepParameters stepParameters =
          NGSectionStepParameters.builder()
              .childNodeId(stepYamlFields.get(0).getNode().getUuid())
              .logMessage("Step Group rollback " + stepYamlFields.get(0).getNode().getIdentifier())
              .build();

      PlanNode stepGroupRollbackNode =
          PlanNode.builder()
              .uuid(rollbackStepsNode.getNode().getUuid())
              .name(stepGroup.getNode().getNameOrIdentifier() + OrchestrationConstants.ROLLBACK_NODE_NAME)
              .identifier(stepGroup.getNode().getIdentifier() + OrchestrationConstants.ROLLBACK_NODE_NAME)
              .stepType(StepGroupRollbackStep.STEP_TYPE)
              .group(StepOutcomeGroup.STEP.name())
              .stepParameters(stepParameters)
              .facilitatorObtainment(
                  FacilitatorObtainment.newBuilder()
                      .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                      .build())
              .build();
      return planCreationResponseBuilder.node(stepGroupRollbackNode.getUuid(), stepGroupRollbackNode).build();
    }

    return PlanCreationResponse.builder().build();
  }

  private static List<YamlField> getStepYamlFields(YamlField rollbackStepsNode) {
    List<YamlNode> yamlNodes =
        Optional.of(Preconditions.checkNotNull(rollbackStepsNode).getNode().asArray()).orElse(Collections.emptyList());
    return PlanCreatorUtils.getStepYamlFields(yamlNodes);
  }
}
