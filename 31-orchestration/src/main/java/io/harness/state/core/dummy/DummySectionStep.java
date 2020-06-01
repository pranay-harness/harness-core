package io.harness.state.core.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Produces(Step.class)
public class DummySectionStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("DUMMY_SECTION").build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    DummySectionStepParameters parameters = (DummySectionStepParameters) stepParameters;
    return ChildExecutableResponse.builder().childNodeId(parameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    DummySectionStepParameters parameters = (DummySectionStepParameters) stepParameters;
    StepResponseBuilder responseBuilder = StepResponse.builder().stepOutcome(
        StepResponse.StepOutcome.builder()
            .name("outcomeData")
            .outcome(DummySectionOutcome.builder()
                         .map(parameters.getData() == null ? null : parameters.getData().getMap())
                         .build())
            .build());
    StatusNotifyResponseData statusNotifyResponseData =
        (StatusNotifyResponseData) responseDataMap.values().iterator().next();
    responseBuilder.status(statusNotifyResponseData.getStatus());
    return responseBuilder.build();
  }
}
