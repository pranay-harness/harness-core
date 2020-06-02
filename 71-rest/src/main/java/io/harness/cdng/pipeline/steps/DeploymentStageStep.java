package io.harness.cdng.pipeline.steps;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
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
public class DeploymentStageStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("DEPLOYMENT_STAGE_STEP").build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    DeploymentStageStepParameters parameters = (DeploymentStageStepParameters) stepParameters;
    logger.info("Executing deployment stage with params [{}]", parameters);

    final Map<String, String> fieldToExecutionNodeIdMap = parameters.getFieldToExecutionNodeIdMap();
    final String executionNodeId = fieldToExecutionNodeIdMap.get("execution");
    return ChildExecutableResponse.builder().childNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    final DeploymentStageStepParameters parameters = (DeploymentStageStepParameters) stepParameters;

    logger.info("executed deployment stage =[{}]", parameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}
