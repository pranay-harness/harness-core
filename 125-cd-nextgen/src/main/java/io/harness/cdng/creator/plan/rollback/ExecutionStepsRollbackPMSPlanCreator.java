/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.rollback.steps.RollbackStepsStep;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
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
import java.util.Set;

/**
 * This class is used to create rollback plan for steps inside rollback section of execution.
 * Example :
 * execution:
 *    steps:
 *    rollbackSteps: // This section
 */
@OwnedBy(HarnessTeam.CDC)
public class ExecutionStepsRollbackPMSPlanCreator implements PartialPlanCreator<YamlField> {
  private static List<YamlField> getStepYamlFields(YamlField rollbackStepsNode) {
    List<YamlNode> yamlNodes =
        Optional.of(Preconditions.checkNotNull(rollbackStepsNode).getNode().asArray()).orElse(Collections.emptyList());
    return PlanCreatorUtils.getStepYamlFields(yamlNodes);
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

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField rollbackStepsField) {
    if (rollbackStepsField == null || rollbackStepsField.getNode().asArray().size() == 0) {
      return PlanCreationResponse.builder().build();
    }
    List<YamlField> stepsArrayFields = getStepYamlFields(rollbackStepsField);
    if (stepsArrayFields.isEmpty()) {
      return PlanCreationResponse.builder().build();
    }

    PlanCreationResponseBuilder planCreationResponseBuilder = PlanCreationResponse.builder();
    Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
    for (YamlField stepYamlField : stepsArrayFields) {
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
    }
    planCreationResponseBuilder.dependencies(DependenciesUtils.toDependenciesProto(stepYamlFieldMap));

    StepParameters stepParameters = NGSectionStepParameters.builder()
                                        .childNodeId(stepsArrayFields.get(0).getNode().getUuid())
                                        .logMessage("Execution Rollback")
                                        .build();
    PlanNode executionRollbackNode =
        PlanNode.builder()
            .uuid(rollbackStepsField.getNode().getUuid() + OrchestrationConstants.ROLLBACK_STEPS_NODE_ID_SUFFIX)
            .name(OrchestrationConstants.ROLLBACK_NODE_NAME)
            .identifier(YAMLFieldNameConstants.ROLLBACK_STEPS)
            .stepType(RollbackStepsStep.STEP_TYPE)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .skipGraphType(SkipType.SKIP_NODE)
            .build();
    return planCreationResponseBuilder.planNode(executionRollbackNode).build();
  }
}
