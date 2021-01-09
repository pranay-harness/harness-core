package io.harness.registrars;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PipelineServiceStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();
    engineSteps.putAll(OrchestrationStepsModuleStepRegistrar.getEngineSteps());
    return engineSteps;
  }
}
