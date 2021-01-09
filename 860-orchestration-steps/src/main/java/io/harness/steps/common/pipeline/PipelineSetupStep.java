package io.harness.steps.common.pipeline;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.plancreator.beans.VariablesOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineSetupStep implements ChildExecutable<PipelineSetupStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.PIPELINE_SECTION).build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for Pipeline Step [{}]", stepParameters);

    final String stagesNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, PipelineSetupStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed Pipeline Step =[{}]", stepParameters);
    StepResponse childResponse = createStepResponseFromChildResponse(responseDataMap);
    VariablesOutcome variablesOutcome = new VariablesOutcome();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(stepParameters.getOriginalVariables(),
        Integer.parseInt(ambiance.getSetupAbstractionsMap().get("expressionFunctorToken"))));
    return StepResponse.builder()
        .status(childResponse.getStatus())
        .failureInfo(childResponse.getFailureInfo())
        .stepOutcome(
            StepResponse.StepOutcome.builder().name(YAMLFieldNameConstants.VARIABLES).outcome(variablesOutcome).build())
        .build();
  }

  @Override
  public Class<PipelineSetupStepParameters> getStepParametersClass() {
    return PipelineSetupStepParameters.class;
  }
}
