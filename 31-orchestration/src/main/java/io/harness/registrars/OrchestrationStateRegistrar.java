package io.harness.registrars;

import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.core.dummy.DummyState;
import io.harness.state.core.fork.ForkState;
import io.harness.state.core.section.SectionState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class OrchestrationStateRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends State>>> stateClasses) {
    stateClasses.add(Pair.of(ForkState.STATE_TYPE, ForkState.class));
    stateClasses.add(Pair.of(SectionState.STATE_TYPE, SectionState.class));
    stateClasses.add(Pair.of(DummyState.STATE_TYPE, DummyState.class));
  }
}
