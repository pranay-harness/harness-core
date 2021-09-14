/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.MapStepParameters;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class StepsStep implements ChildrenExecutable<MapStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("steps").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<MapStepParameters> getStepParametersClass() {
    return MapStepParameters.class;
  }

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, MapStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Steps Step parameters: {}", RecastOrchestrationUtils.toJson(stepParameters));
    List<String> childrenNodeIds = (List<String>) stepParameters.get("childrenNodeIds");
    List<Child> children = new ArrayList<>();
    if (childrenNodeIds != null) {
      childrenNodeIds.forEach(id -> children.add(Child.newBuilder().setChildNodeId(id).build()));
    }
    return ChildrenExecutableResponse.newBuilder().addAllChildren(children).build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, MapStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Steps response map: {}", RecastOrchestrationUtils.toJson(responseDataMap));
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
