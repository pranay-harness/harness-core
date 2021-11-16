package io.harness.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.OrchestrationStepTypes;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
// TODO : Make this abstract remove step type let individual services override it
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class NGSectionStep implements ChildExecutable<NGSectionStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.NG_SECTION).setStepCategory(StepCategory.STEP).build();

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, NGSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, NGSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<NGSectionStepParameters> getStepParametersClass() {
    return NGSectionStepParameters.class;
  }
}
