package io.harness.cdng.infra.steps;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InfrastructureSectionStep implements ChildExecutable<InfraSectionStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.INFRASTRUCTURE_SECTION.getName()).build();

  @Override
  public Class<InfraSectionStepParameters> getStepParametersClass() {
    return InfraSectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for InfraSection Step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeID()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for InfraSection Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }
}
