package io.harness.state.core.section;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
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
@Produces(State.class)
public class SectionState implements State, ChildExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type("SECTION").build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    SectionStateParameters stateParameters = (SectionStateParameters) parameters;
    return ChildExecutableResponse.builder().childNodeId(stateParameters.getChildNodeId()).build();
  }

  @Override
  public StateResponse handleChildResponse(
      Ambiance ambiance, StateParameters stateParameters, Map<String, ResponseData> responseDataMap) {
    StateResponseBuilder responseBuilder = StateResponse.builder();
    StatusNotifyResponseData statusNotifyResponseData =
        (StatusNotifyResponseData) responseDataMap.values().iterator().next();
    responseBuilder.status(statusNotifyResponseData.getStatus());
    return responseBuilder.build();
  }

  @Override
  public StateType getType() {
    return STATE_TYPE;
  }
}
