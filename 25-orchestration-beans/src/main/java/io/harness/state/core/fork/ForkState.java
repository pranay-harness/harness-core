package io.harness.state.core.fork;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.Child;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.StateResponseBuilder;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Produces(State.class)
public class ForkState implements State, ChildrenExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type("FORK").build();

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    ForkStateParameters forkStateParameters = (ForkStateParameters) parameters;
    ChildrenExecutableResponseBuilder responseBuilder = ChildrenExecutableResponse.builder();
    for (String nodeId : forkStateParameters.getParallelNodeIds()) {
      responseBuilder.child(Child.builder().childNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StateResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters stateParameters, Map<String, ResponseData> responseDataMap) {
    StateResponseBuilder responseBuilder = StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED);
    for (ResponseData responseData : responseDataMap.values()) {
      NodeExecutionStatus executionStatus = ((StatusNotifyResponseData) responseData).getStatus();
      if (executionStatus != NodeExecutionStatus.SUCCEEDED) {
        responseBuilder.status(executionStatus);
      }
    }
    return responseBuilder.build();
  }

  @Override
  public StateType getType() {
    return STATE_TYPE;
  }
}
