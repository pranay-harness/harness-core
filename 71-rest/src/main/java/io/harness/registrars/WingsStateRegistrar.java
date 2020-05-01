package io.harness.registrars;

import io.harness.redesign.states.dummy.DummyState;
import io.harness.redesign.states.http.BasicHttpState;
import io.harness.redesign.states.wait.WaitState;
import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;

import java.util.Set;

public class WingsStateRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Class<? extends State>> stateClasses) {
    stateClasses.add(BasicHttpState.class);
    stateClasses.add(WaitState.class);
    stateClasses.add(DummyState.class);
  }
}
