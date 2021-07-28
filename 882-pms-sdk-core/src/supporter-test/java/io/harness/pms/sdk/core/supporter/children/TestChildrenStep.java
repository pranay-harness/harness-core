package io.harness.pms.sdk.core.supporter.children;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(CDC)
public class TestChildrenStep implements ChildrenExecutable<TestChildrenStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("TEST_CHILDREN").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<TestChildrenStepParameters> getStepParametersClass() {
    return TestChildrenStepParameters.class;
  }

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, TestChildrenStepParameters forkStepParameters, StepInputPackage inputPackage) {
    ChildrenExecutableResponse.Builder responseBuilder = ChildrenExecutableResponse.newBuilder();
    for (String nodeId : forkStepParameters.getParallelNodeIds()) {
      responseBuilder.addChildren(Child.newBuilder().setChildNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, TestChildrenStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    Status status = Status.SUCCEEDED;
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (StatusUtils.brokeStatuses().contains(executionStatus)) {
        status = executionStatus;
      }
    }

    return StepResponse.builder().status(status).build();
  }
}
