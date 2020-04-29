package io.harness.state;

import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;

@Redesign
public interface State extends RegistrableEntity<StateType> {
  // MetaData management to the Designer
  StateType getType();
}
