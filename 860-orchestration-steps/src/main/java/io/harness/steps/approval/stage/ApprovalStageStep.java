package io.harness.steps.approval.stage;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.beans.VariablesSweepingOutput;
import io.harness.plancreator.steps.common.StageElementParameters;
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

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class ApprovalStageStep implements ChildExecutable<StageElementParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.APPROVAL_STAGE).build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<StageElementParameters> getStepParametersClass() {
    return StageElementParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StageElementParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing approval stage with params [{}]", stepParameters);
    ApprovalStageSpecParameters specParameters = (ApprovalStageSpecParameters) stepParameters.getSpec();
    String executionNodeId = specParameters.getChildNodeID();
    VariablesSweepingOutput variablesSweepingOutput = getVariablesSweepingOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, StepOutcomeGroup.STAGE.name());
    return ChildExecutableResponse.newBuilder().setChildNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StageElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed approval stage [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @NotNull
  private VariablesSweepingOutput getVariablesSweepingOutput(Ambiance ambiance, StageElementParameters stepParameters) {
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), ambiance.getExpressionFunctorToken()));
    return variablesOutcome;
  }
}
