package io.harness.pms.sample.cd;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sample.steps.*;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.pipeline.PipelineSetupStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CdServiceStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();
    engineSteps.put(StageStep.STEP_TYPE, StageStep.class);
    engineSteps.put(StepsStep.STEP_TYPE, StepsStep.class);
    engineSteps.put(ServiceStep.STEP_TYPE, ServiceStep.class);
    engineSteps.put(InfrastructureStep.STEP_TYPE, InfrastructureStep.class);
    engineSteps.put(K8sRollingStep.STEP_TYPE, K8sRollingStep.class);
    engineSteps.put(K8sCanaryStep.STEP_TYPE, K8sCanaryStep.class);
    engineSteps.put(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class);
    engineSteps.put(NGSectionStep.STEP_TYPE, NGSectionStep.class);
    return engineSteps;
  }
}
