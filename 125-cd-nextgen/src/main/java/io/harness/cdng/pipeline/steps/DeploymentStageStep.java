package io.harness.cdng.pipeline.steps;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.beans.VariablesOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentStageStep implements ChildExecutable<DeploymentStageStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.DEPLOYMENT_STAGE_STEP.getName()).build();

  @Override
  public Class<DeploymentStageStepParameters> getStepParametersClass() {
    return DeploymentStageStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing deployment stage with params [{}]", stepParameters);

    final String serviceNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(serviceNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed deployment stage =[{}]", stepParameters);

    StepResponse childResponse = createStepResponseFromChildResponse(responseDataMap);

    VariablesOutcome variablesOutcome = new VariablesOutcome();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), Integer.parseInt(AmbianceHelper.getExpressionFunctorToken(ambiance))));
    return StepResponse.builder()
        .status(childResponse.getStatus())
        .failureInfo(childResponse.getFailureInfo())
        .stepOutcome(StepOutcome.builder().name(YAMLFieldNameConstants.VARIABLES).outcome(variablesOutcome).build())
        .build();
  }
}
