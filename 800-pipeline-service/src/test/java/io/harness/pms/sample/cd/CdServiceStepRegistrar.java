package io.harness.pms.sample.cd;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sample.steps.InfrastructureStep;
import io.harness.pms.sample.steps.K8sCanaryStep;
import io.harness.pms.sample.steps.K8sRollingStep;
import io.harness.pms.sample.steps.ServiceStep;
import io.harness.pms.sample.steps.StageStep;
import io.harness.pms.sample.steps.StepsStep;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.pipeline.PipelineSetupStep;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CdServiceStepRegistrar {
  public Map<StepType, Step> getEngineSteps(Injector injector) {
    Map<StepType, Step> engineSteps = new HashMap<>();
    engineSteps.put(StageStep.STEP_TYPE, injector.getInstance(StageStep.class));
    engineSteps.put(StepsStep.STEP_TYPE, injector.getInstance(StepsStep.class));
    engineSteps.put(ServiceStep.STEP_TYPE, injector.getInstance(ServiceStep.class));
    engineSteps.put(InfrastructureStep.STEP_TYPE, injector.getInstance(InfrastructureStep.class));
    engineSteps.put(K8sRollingStep.STEP_TYPE, injector.getInstance(K8sRollingStep.class));
    engineSteps.put(K8sCanaryStep.STEP_TYPE, injector.getInstance(K8sCanaryStep.class));
    engineSteps.put(PipelineSetupStep.STEP_TYPE, injector.getInstance(PipelineSetupStep.class));
    engineSteps.put(NGSectionStep.STEP_TYPE, injector.getInstance(NGSectionStep.class));
    return engineSteps;
  }
}
