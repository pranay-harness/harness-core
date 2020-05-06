package io.harness.registrars;

import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;
import io.harness.states.BuildEnvSetupState;

import java.util.Set;

public class ExecutionRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Class<? extends State>> stateClasses) {
    stateClasses.add(BuildEnvSetupState.class);
  }
}
