package io.harness.redesign.states.http;

import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.registries.state.StateProducer;
import io.harness.state.State;
import io.harness.state.StateType;

@Redesign
@Produces(State.class)
public class BasicHttpStateProducer implements StateProducer {
  @Override
  public State produce() {
    return new BasicHttpState();
  }

  @Override
  public StateType getType() {
    return StateType.builder().type(BasicHttpState.STATE_TYPE).build();
  }
}
