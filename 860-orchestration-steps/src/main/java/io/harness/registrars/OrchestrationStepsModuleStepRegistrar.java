package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.registries.registrar.StepRegistrar;
import io.harness.pms.steps.StepType;
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

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class OrchestrationStepsModuleStepRegistrar implements StepRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<StepType, Step>> stepClasses) {
    stepClasses.add(Pair.of(BarrierStep.STEP_TYPE, injector.getInstance(BarrierStep.class)));
    stepClasses.add(Pair.of(ResourceRestraintStep.STEP_TYPE, injector.getInstance(ResourceRestraintStep.class)));
    stepClasses.add(Pair.of(ForkStep.STEP_TYPE, injector.getInstance(ForkStep.class)));
    stepClasses.add(Pair.of(SectionStep.STEP_TYPE, injector.getInstance(SectionStep.class)));
    stepClasses.add(Pair.of(DummyStep.STEP_TYPE, injector.getInstance(DummyStep.class)));
    stepClasses.add(Pair.of(SectionChainStep.STEP_TYPE, injector.getInstance(SectionChainStep.class)));
    stepClasses.add(Pair.of(DummySectionStep.STEP_TYPE, injector.getInstance(DummySectionStep.class)));
    stepClasses.add(Pair.of(PipelineSetupStep.STEP_TYPE, injector.getInstance(PipelineSetupStep.class)));
    stepClasses.add(Pair.of(StepGroupStep.STEP_TYPE, injector.getInstance(StepGroupStep.class)));
    stepClasses.add(Pair.of(NGForkStep.STEP_TYPE, injector.getInstance(NGForkStep.class)));
    stepClasses.add(Pair.of(NGSectionStep.STEP_TYPE, injector.getInstance(NGSectionStep.class)));
  }
}
