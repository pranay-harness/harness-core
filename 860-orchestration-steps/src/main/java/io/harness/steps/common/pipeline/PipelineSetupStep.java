package io.harness.steps.common.pipeline;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineSetupStep implements ChildExecutable<PipelineSetupStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.PIPELINE_SECTION).build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for pipeline [{}]", stepParameters);

    final String stagesNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.builder().childNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed pipeline =[{}]", stepParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<PipelineSetupStepParameters> getStepParametersClass() {
    return PipelineSetupStepParameters.class;
  }
}
