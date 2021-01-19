package io.harness.cdng.pipeline.steps;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.beans.VariablesSweepingOutput;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class DeploymentStageStep implements ChildExecutable<DeploymentStageStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.DEPLOYMENT_STAGE_STEP.getName()).build();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<DeploymentStageStepParameters> getStepParametersClass() {
    return DeploymentStageStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing deployment stage with params [{}]", stepParameters);

    final String serviceNodeId = stepParameters.getChildNodeID();
    VariablesSweepingOutput variablesSweepingOutput = getVariablesSweepingOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, StepOutcomeGroup.STAGE.name());

    return ChildExecutableResponse.newBuilder().setChildNodeId(serviceNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed deployment stage =[{}]", stepParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }

  @NotNull
  private VariablesSweepingOutput getVariablesSweepingOutput(
      Ambiance ambiance, DeploymentStageStepParameters stepParameters) {
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), Integer.parseInt(AmbianceHelper.getExpressionFunctorToken(ambiance))));
    return variablesOutcome;
  }
}
