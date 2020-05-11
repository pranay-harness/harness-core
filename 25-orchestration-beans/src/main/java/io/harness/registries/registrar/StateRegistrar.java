package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.State;

import java.util.Set;

@OwnedBy(CDC)
public interface StateRegistrar extends EngineRegistrar<State> {
  void register(Set<Class<? extends State>> stateClasses);
}
