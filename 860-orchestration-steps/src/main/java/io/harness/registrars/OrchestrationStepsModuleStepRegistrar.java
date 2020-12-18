package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.common.NGForkStep;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.common.steps.stepgroup.StepGroupStep;
import io.harness.steps.dummy.DummySectionStep;
import io.harness.steps.dummy.DummyStep;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;
import io.harness.steps.section.SectionStep;
import io.harness.steps.section.chain.SectionChainStep;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationStepsModuleStepRegistrar {
  public Map<StepType, Step> getEngineSteps(Injector injector) {
    Map<StepType, Step> engineSteps = new HashMap<>();

    engineSteps.put(BarrierStep.STEP_TYPE, injector.getInstance(BarrierStep.class));
    engineSteps.put(ResourceRestraintStep.STEP_TYPE, injector.getInstance(ResourceRestraintStep.class));
    engineSteps.put(ForkStep.STEP_TYPE, injector.getInstance(ForkStep.class));
    engineSteps.put(SectionStep.STEP_TYPE, injector.getInstance(SectionStep.class));
    engineSteps.put(DummyStep.STEP_TYPE, injector.getInstance(DummyStep.class));
    engineSteps.put(SectionChainStep.STEP_TYPE, injector.getInstance(SectionChainStep.class));
    engineSteps.put(DummySectionStep.STEP_TYPE, injector.getInstance(DummySectionStep.class));
    engineSteps.put(PipelineSetupStep.STEP_TYPE, injector.getInstance(PipelineSetupStep.class));
    engineSteps.put(StepGroupStep.STEP_TYPE, injector.getInstance(StepGroupStep.class));
    engineSteps.put(NGForkStep.STEP_TYPE, injector.getInstance(NGForkStep.class));
    engineSteps.put(NGSectionStep.STEP_TYPE, injector.getInstance(NGSectionStep.class));

    return engineSteps;
  }
}
