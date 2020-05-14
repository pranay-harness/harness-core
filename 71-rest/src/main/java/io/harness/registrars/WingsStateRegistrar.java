package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.states.http.BasicHttpState;
import io.harness.redesign.states.wait.WaitState;
import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;
import io.harness.state.StateType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class WingsStateRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends State>>> stateClasses) {
    stateClasses.add(Pair.of(BasicHttpState.STATE_TYPE, BasicHttpState.class));
    stateClasses.add(Pair.of(WaitState.STATE_TYPE, WaitState.class));
  }
}
