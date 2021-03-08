package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationStepsModuleStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    engineSteps.put(BarrierStep.STEP_TYPE, BarrierStep.class);
    engineSteps.put(ResourceRestraintStep.STEP_TYPE, ResourceRestraintStep.class);
    engineSteps.put(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class);

    engineSteps.putAll(OrchestrationStepsModuleSdkStepRegistrar.getEngineSteps());

    return engineSteps;
  }
}
