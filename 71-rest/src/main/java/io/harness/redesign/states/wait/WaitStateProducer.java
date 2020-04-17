package io.harness.redesign.states.wait;

import io.harness.annotations.ProducesState;
import io.harness.registries.state.StateProducer;
import io.harness.state.State;
import software.wings.sm.StateType;

@ProducesState
public class WaitStateProducer implements StateProducer {
  @Override
  public State produce() {
    return new WaitState();
  }

  @Override
  public String getType() {
    return StateType.WAIT.name();
  }
}
