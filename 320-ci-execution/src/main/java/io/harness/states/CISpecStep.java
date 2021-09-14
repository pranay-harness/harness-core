/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.states;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CISpecStep implements ChildExecutable<IntegrationStageStepParametersPMS> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("CISPECPMS").setStepCategory(StepCategory.STEP).build();
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, IntegrationStageStepParametersPMS stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for spec node step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeID()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, IntegrationStageStepParametersPMS stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for spec node step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<IntegrationStageStepParametersPMS> getStepParametersClass() {
    return IntegrationStageStepParametersPMS.class;
  }
}
