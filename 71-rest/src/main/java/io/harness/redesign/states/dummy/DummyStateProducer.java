package io.harness.redesign.states.dummy;

import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.registries.state.StateProducer;
import io.harness.state.State;
import io.harness.state.StateType;

@Redesign
@Produces(State.class)
public class DummyStateProducer implements StateProducer {
  @Override
  public State produce() {
    return new DummyState();
  }

  @Override
  public StateType getType() {
    return StateType.builder().type(DummyState.STATE_TYPE).build();
  }
}
