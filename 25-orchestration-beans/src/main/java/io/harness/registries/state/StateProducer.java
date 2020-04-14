package io.harness.registries.state;

import io.harness.annotations.Redesign;
import io.harness.state.State;

@Redesign
public interface StateProducer {
  State produce();

  String getType();
}
