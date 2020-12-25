package io.harness.pms.sample.cv;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sample.steps.AppdVerifyStep;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.common.pipeline.PipelineSetupStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CvServiceStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();
    engineSteps.put(AppdVerifyStep.STEP_TYPE, AppdVerifyStep.class);
    engineSteps.put(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class);
    return engineSteps;
  }
}
