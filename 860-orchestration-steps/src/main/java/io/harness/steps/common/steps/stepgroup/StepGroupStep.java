package io.harness.steps.common.steps.stepgroup;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepGroupStep implements ChildExecutable<StepGroupStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.STEP_GROUP).build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepGroupStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting StepGroup for Pipeline Step [{}]", stepParameters);
    final String stepNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepGroupStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed StepGroup Step =[{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<StepGroupStepParameters> getStepParametersClass() {
    return StepGroupStepParameters.class;
  }
}
