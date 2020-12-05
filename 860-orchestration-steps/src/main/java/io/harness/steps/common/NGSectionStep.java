package io.harness.steps.common;

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
public class NGSectionStep implements ChildExecutable<NGSectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.NG_SECTION).build();
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, NGSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, NGSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<NGSectionStepParameters> getStepParametersClass() {
    return NGSectionStepParameters.class;
  }
}
