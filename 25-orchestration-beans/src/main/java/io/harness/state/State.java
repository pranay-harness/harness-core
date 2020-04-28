package io.harness.state;

import io.harness.annotations.Redesign;

@Redesign
public interface State {
  // MetaData management to the Designer
  StateType getType();
}
