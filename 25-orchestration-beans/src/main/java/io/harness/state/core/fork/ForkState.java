package io.harness.state.core.fork;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.modes.children.ChildrenExecutable;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse.Child;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.State;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;

import java.util.List;
import java.util.Map;

@Redesign
public class ForkState implements State, ChildrenExecutable {
  @Override
  public String getStateType() {
    return "FORK";
  }

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
    return StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
  }
}
