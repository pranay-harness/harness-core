package io.harness.steps.common;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildrenExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NGForkStep implements ChildrenExecutable<ForkStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.NG_FORK).build();

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, ForkStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for Parallel Step [{}]", stepParameters);
    ChildrenExecutableResponse.Builder responseBuilder = ChildrenExecutableResponse.newBuilder();
    for (String nodeId : stepParameters.getParallelNodeIds()) {
      responseBuilder.addChildren(ChildrenExecutableResponse.Child.newBuilder().setChildNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, ForkStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed  execution for Parallel Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<ForkStepParameters> getStepParametersClass() {
    return ForkStepParameters.class;
  }
}
