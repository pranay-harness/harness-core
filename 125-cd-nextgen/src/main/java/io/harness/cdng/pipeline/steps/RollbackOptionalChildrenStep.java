package io.harness.cdng.pipeline.steps;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.plancreators.PlanCreatorHelper;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class RollbackOptionalChildrenStep implements Step, ChildrenExecutable<RollbackOptionalChildrenParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("ROLLBACK_OPTIONAL_CHILDREN").build();

  @Inject private PlanCreatorHelper planCreatorHelper;

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, RollbackOptionalChildrenParameters stepParameters, StepInputPackage inputPackage) {
    ChildrenExecutableResponseBuilder responseBuilder = ChildrenExecutableResponse.builder();
    for (RollbackNode node : stepParameters.getParallelNodes()) {
      if (planCreatorHelper.shouldNodeRun(node, ambiance)) {
        responseBuilder.child(ChildrenExecutableResponse.Child.builder().childNodeId(node.getNodeId()).build());
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
