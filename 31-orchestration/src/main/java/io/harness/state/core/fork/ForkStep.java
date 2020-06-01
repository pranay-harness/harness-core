package io.harness.state.core.fork;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.Child;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Produces(Step.class)
public class ForkStep implements Step, ChildrenExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("FORK").build();

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    ForkStepParameters parameters = (ForkStepParameters) stepParameters;
    ChildrenExecutableResponseBuilder responseBuilder = ChildrenExecutableResponse.builder();
    for (String nodeId : parameters.getParallelNodeIds()) {
      responseBuilder.child(Child.builder().childNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
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
