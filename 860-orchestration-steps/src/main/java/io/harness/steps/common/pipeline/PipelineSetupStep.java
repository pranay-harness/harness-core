package io.harness.steps.common.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.VariablesSweepingOutput;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(PIPELINE)
public class PipelineSetupStep implements ChildExecutable<PipelineSetupStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.PIPELINE_SECTION).build();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for Pipeline Step [{}]", stepParameters);

    final String stagesNodeId = stepParameters.getChildNodeID();
    VariablesSweepingOutput variablesSweepingOutput = getVariablesOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, StepOutcomeGroup.PIPELINE.name());

    return ChildExecutableResponse.newBuilder().setChildNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed Pipeline Step =[{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @NotNull
  private VariablesSweepingOutput getVariablesOutput(Ambiance ambiance, PipelineSetupStepParameters stepParameters) {
    VariablesSweepingOutput variablesSweepingOutput = new VariablesSweepingOutput();
    variablesSweepingOutput.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), ambiance.getExpressionFunctorToken()));
    return variablesSweepingOutput;
  }

  @Override
  public Class<PipelineSetupStepParameters> getStepParametersClass() {
    return PipelineSetupStepParameters.class;
  }
}
