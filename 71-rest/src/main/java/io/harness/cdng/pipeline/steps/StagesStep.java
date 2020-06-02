package io.harness.cdng.pipeline.steps;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.StagesStepParameters;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class StagesStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("STAGES_SETUP").build();
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    StagesStepParameters parameters = (StagesStepParameters) stepParameters;
    logger.info("starting execution for stages [{}]", parameters);
    // TODO @rk: 27/05/20 : support multiple stages
    final String stageNodeId = parameters.getStageNodeIds().get(0);
    return ChildExecutableResponse.builder().childNodeId(stageNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return createStepResponseFromChildResponse(responseDataMap);
  }
}
