package io.harness.state;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateMetadata;

@Redesign
public interface State {
  String getStateType();

  StateMetadata getMetadata();
}
