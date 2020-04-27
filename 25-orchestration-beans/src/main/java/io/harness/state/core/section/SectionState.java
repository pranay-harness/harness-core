package io.harness.state.core.section;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.modes.child.ChildExecutable;
import io.harness.facilitate.modes.child.ChildExecutableResponse;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.StateResponseBuilder;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;

import java.util.List;
import java.util.Map;

public class SectionState implements State, ChildExecutable {
  public static final String STATE_TYPE = "SECTION";

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    SectionStateParameters stateParameters = (SectionStateParameters) parameters;
    return ChildExecutableResponse.builder().childNodeId(stateParameters.getChildNodeId()).build();
  }

  @Override
  public StateResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters stateParameters, Map<String, ResponseData> responseDataMap) {
    StateResponseBuilder responseBuilder = StateResponse.builder();
    StatusNotifyResponseData statusNotifyResponseData =
        (StatusNotifyResponseData) responseDataMap.values().iterator().next();
    responseBuilder.status(statusNotifyResponseData.getStatus());
    return responseBuilder.build();
  }

  @Override
  public StateType getStateType() {
    return StateType.builder().type(STATE_TYPE).build();
  }
}
