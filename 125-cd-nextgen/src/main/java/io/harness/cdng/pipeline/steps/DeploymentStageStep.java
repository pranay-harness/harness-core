package io.harness.cdng.pipeline.steps;

import static io.harness.ngpipeline.orchestration.StepUtils.createStepResponseFromChildResponse;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentStageStep implements Step, ChildExecutable<DeploymentStageStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.DEPLOYMENT_STAGE_STEP.getName()).build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing deployment stage with params [{}]", stepParameters);

    final Map<String, String> fieldToExecutionNodeIdMap = stepParameters.getFieldToExecutionNodeIdMap();
    final String executionNodeId = fieldToExecutionNodeIdMap.get("execution");
    return ChildExecutableResponse.builder().childNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed deployment stage =[{}]", stepParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}
