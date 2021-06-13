package io.harness.pms.sdk.core.supporter.child;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(PIPELINE)
public class TestSectionStep implements ChildExecutable<TestSectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("TEST_CHILD").build();

  @Override
  public Class<TestSectionStepParameters> getStepParametersClass() {
    return TestSectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, TestSectionStepParameters dummySectionStepParameters, StepInputPackage inputPackage) {
    return ChildExecutableResponse.newBuilder().setChildNodeId(dummySectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(Ambiance ambiance, TestSectionStepParameters dummySectionStepParameters,
      Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().stepOutcome(
        StepResponse.StepOutcome.builder()
            .name("outcomeData")
            .outcome(TestSectionOutcome.builder().map(dummySectionStepParameters.getData()).build())
            .build());
    StepResponseNotifyData stepResponseNotifyData = (StepResponseNotifyData) responseDataMap.values().iterator().next();
    responseBuilder.status(stepResponseNotifyData.getStatus());
    return responseBuilder.build();
  }
}
