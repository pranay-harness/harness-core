package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.core.barrier.BarrierStep;
import io.harness.state.core.dummy.DummySectionStep;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.section.SectionStep;
import io.harness.state.core.section.chain.SectionChainStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stepClasses) {
    stepClasses.add(Pair.of(ForkStep.STEP_TYPE, ForkStep.class));
    stepClasses.add(Pair.of(SectionStep.STEP_TYPE, SectionStep.class));
    stepClasses.add(Pair.of(DummyStep.STEP_TYPE, DummyStep.class));
    stepClasses.add(Pair.of(SectionChainStep.STEP_TYPE, SectionChainStep.class));
    stepClasses.add(Pair.of(DummySectionStep.STEP_TYPE, DummySectionStep.class));
    stepClasses.add(Pair.of(BarrierStep.STEP_TYPE, BarrierStep.class));
  }
}
