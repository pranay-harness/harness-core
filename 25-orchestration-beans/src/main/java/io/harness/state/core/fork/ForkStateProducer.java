package io.harness.state.core.fork;

import io.harness.annotations.ProducesState;
import io.harness.annotations.Redesign;
import io.harness.registries.state.StateProducer;
import io.harness.state.State;

@Redesign
@ProducesState
public class ForkStateProducer implements StateProducer {
  @Override
  public State produce() {
    return new ForkState();
  }

  @Override
  public String getType() {
    return "FORK";
  }
}
