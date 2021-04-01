package io.harness.steps.approval.stage;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.beans.VariablesSweepingOutput;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(PIPELINE)
public class ApprovalStageStep implements ChildExecutable<ApprovalStageStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.APPROVAL_STAGE).build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<ApprovalStageStepParameters> getStepParametersClass() {
    return ApprovalStageStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, ApprovalStageStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing approval stage with params [{}]", stepParameters);
    String executionNodeId = stepParameters.getChildNodeID();
    VariablesSweepingOutput variablesSweepingOutput = getVariablesSweepingOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, StepOutcomeGroup.STAGE.name());
    return ChildExecutableResponse.newBuilder().setChildNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, ApprovalStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed approval stage [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @NotNull
  private VariablesSweepingOutput getVariablesSweepingOutput(
      Ambiance ambiance, ApprovalStageStepParameters stepParameters) {
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), ambiance.getExpressionFunctorToken()));
    return variablesOutcome;
  }
}
