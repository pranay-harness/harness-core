package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(CDC)
public class DummySectionStep implements ChildExecutable<DummySectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.DUMMY_SECTION).build();

  @Override
  public Class<DummySectionStepParameters> getStepParametersClass() {
    return DummySectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, DummySectionStepParameters dummySectionStepParameters, StepInputPackage inputPackage) {
    return ChildExecutableResponse.builder().childNodeId(dummySectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(Ambiance ambiance, DummySectionStepParameters dummySectionStepParameters,
      Map<String, ResponseData> responseDataMap) {
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
