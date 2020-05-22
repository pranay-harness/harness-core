package io.harness.registrars;

import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.section.SectionStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class OrchestrationStateRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(ForkStep.STATE_TYPE, ForkStep.class));
    stateClasses.add(Pair.of(SectionStep.STATE_TYPE, SectionStep.class));
    stateClasses.add(Pair.of(DummyStep.STATE_TYPE, DummyStep.class));
  }
}
