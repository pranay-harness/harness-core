package io.harness.state.core.section;

import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.registries.state.StateProducer;
import io.harness.state.State;
import io.harness.state.StateType;

@Redesign
@Produces(State.class)
public class SectionStateProducer implements StateProducer {
  @Override
  public State produce() {
    return new SectionState();
  }

  @Override
  public StateType getType() {
    return StateType.builder().type(SectionState.STATE_TYPE).build();
  }
}
