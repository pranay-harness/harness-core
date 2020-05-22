package io.harness.registrars;

import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.section.SectionStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class OrchestrationStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(ForkStep.STATE_TYPE, ForkStep.class));
    stateClasses.add(Pair.of(SectionStep.STATE_TYPE, SectionStep.class));
    stateClasses.add(Pair.of(DummyStep.STATE_TYPE, DummyStep.class));
  }
}
