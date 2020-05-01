package io.harness.registries.registrar;

import io.harness.state.State;

import java.util.Set;

public interface StateRegistrar extends EngineRegistrar<State> {
  void register(Set<Class<? extends State>> stateClasses);
}
