package io.harness.state.registry;

import io.harness.state.State;
import io.harness.state.metadata.StateMetadata;

public interface StateProducer {
  <T extends State> T produce();

  StateMetadata produceMetadata();
}
