/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.advisers.RollbackCustomAdviser;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.common.NGSectionStepParameters;
import io.harness.steps.common.NGSectionStepWithRollbackInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class CDPlanCreatorUtils {
  public PlanNode getCdStepsNode(String nodeUuid, String childNodeId, String logMessage) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childNodeId).logMessage(logMessage).build();
    return PlanNode.builder()
        .uuid(nodeUuid)
        .identifier(YAMLFieldNameConstants.STEPS)
        .stepType(NGSectionStepWithRollbackInfo.STEP_TYPE)
        .name(YAMLFieldNameConstants.STEPS)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainment(AdviserObtainment.newBuilder().setType(RollbackCustomAdviser.ADVISER_TYPE).build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
