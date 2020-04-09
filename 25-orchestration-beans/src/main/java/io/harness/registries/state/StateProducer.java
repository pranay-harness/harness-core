package io.harness.registries.state;

import io.harness.state.State;

public interface StateProducer {
  State produce();

  String getType();
}
