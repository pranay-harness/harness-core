package io.harness.states;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Produces(Step.class)
@Slf4j
public class IntegrationStageStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("INTEGRATION_STAGE_STEP").build();
  public static final String CHILD_PLAN_START_NODE_NAME = "execution";

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    IntegrationStageStepParameters parameters = (IntegrationStageStepParameters) stepParameters;
    logger.info("Executing deployment stage with params [{}]", parameters);

    final Map<String, String> fieldToExecutionNodeIdMap = parameters.getFieldToExecutionNodeIdMap();

    final String executionNodeId = fieldToExecutionNodeIdMap.get(CHILD_PLAN_START_NODE_NAME);

    return ChildExecutableResponse.builder().childNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    final IntegrationStageStepParameters parameters = (IntegrationStageStepParameters) stepParameters;

    logger.info("executed integration stage =[{}]", parameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}
