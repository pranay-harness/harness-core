package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.dummy.DummySectionStep;
import io.harness.steps.dummy.DummyStep;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;
import io.harness.steps.section.SectionStep;
import io.harness.steps.section.chain.SectionChainStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationStepsModuleStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stepClasses) {
    stepClasses.add(Pair.of(BarrierStep.STEP_TYPE, BarrierStep.class));
    stepClasses.add(Pair.of(ResourceRestraintStep.STEP_TYPE, ResourceRestraintStep.class));
    stepClasses.add(Pair.of(ForkStep.STEP_TYPE, ForkStep.class));
    stepClasses.add(Pair.of(SectionStep.STEP_TYPE, SectionStep.class));
    stepClasses.add(Pair.of(DummyStep.STEP_TYPE, DummyStep.class));
    stepClasses.add(Pair.of(SectionChainStep.STEP_TYPE, SectionChainStep.class));
    stepClasses.add(Pair.of(DummySectionStep.STEP_TYPE, DummySectionStep.class));
  }
}
