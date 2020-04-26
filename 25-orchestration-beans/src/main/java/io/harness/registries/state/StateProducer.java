package io.harness.registries.state;

import io.harness.annotations.Redesign;
import io.harness.state.State;
import io.harness.state.StateType;

@Redesign
public interface StateProducer {
  State produce();

  StateType getType();
}
