package io.harness.cdng.pipeline.steps;

import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.plancreators.PlanCreatorHelper;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildrenExecutableResponse;
import io.harness.pms.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

public class RollbackOptionalChildrenStep implements ChildrenExecutable<RollbackOptionalChildrenParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.ROLLBACK_SECTION.getName()).build();

  @Inject private PlanCreatorHelper planCreatorHelper;

  @Override
  public Class<RollbackOptionalChildrenParameters> getStepParametersClass() {
    return RollbackOptionalChildrenParameters.class;
  }

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, RollbackOptionalChildrenParameters stepParameters, StepInputPackage inputPackage) {
    ChildrenExecutableResponse.Builder responseBuilder = ChildrenExecutableResponse.newBuilder();
    for (RollbackNode node : stepParameters.getParallelNodes()) {
      if (planCreatorHelper.shouldNodeRun(node, ambiance)) {
        responseBuilder.addChildren(Child.newBuilder().setChildNodeId(node.getNodeId()).build());
      }
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, RollbackOptionalChildrenParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    for (ResponseData responseData : responseDataMap.values()) {
      Status executionStatus = ((StepResponseNotifyData) responseData).getStatus();
      if (executionStatus != Status.SUCCEEDED) {
        responseBuilder.status(executionStatus);
      }
    }
    return responseBuilder.build();
  }
}
