package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;
import io.harness.state.core.dummy.DummyState;
import io.harness.state.core.fork.ForkState;
import io.harness.state.core.section.SectionState;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationBeansStateRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Class<? extends State>> stateClasses) {
    stateClasses.add(ForkState.class);
    stateClasses.add(SectionState.class);
    stateClasses.add(DummyState.class);
  }
}
