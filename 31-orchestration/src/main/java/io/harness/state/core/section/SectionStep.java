package io.harness.state.core.section;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;
@OwnedBy(CDC)
@Produces(Step.class)
public class SectionStep implements Step, ChildExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type("SECTION").build();

  @Override
  public ChildExecutableResponse obtainChild(Ambiance ambiance, StateParameters parameters, List<StepTransput> inputs) {
    SectionStateParameters stateParameters = (SectionStateParameters) parameters;
    return ChildExecutableResponse.builder().childNodeId(stateParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StateParameters stateParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
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
