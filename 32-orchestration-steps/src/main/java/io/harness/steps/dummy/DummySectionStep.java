package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;

import java.util.Map;

@OwnedBy(CDC)
public class DummySectionStep implements Step, ChildExecutable<DummySectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("DUMMY_SECTION").build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, DummySectionStepParameters dummySectionStepParameters, StepInputPackage inputPackage) {
    return ChildExecutableResponse.builder().childNodeId(dummySectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(Ambiance ambiance, DummySectionStepParameters dummySectionStepParameters,
      Map<String, DelegateResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().stepOutcome(
        StepResponse.StepOutcome.builder()
            .name("outcomeData")
            .outcome(DummySectionOutcome.builder().map(dummySectionStepParameters.getData()).build())
            .build());
    StepResponseNotifyData stepResponseNotifyData = (StepResponseNotifyData) responseDataMap.values().iterator().next();
    responseBuilder.status(stepResponseNotifyData.getStatus());
    return responseBuilder.build();
  }
}
