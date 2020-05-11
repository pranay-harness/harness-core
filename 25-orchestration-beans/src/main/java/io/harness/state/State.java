package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(CDC)
@Redesign
public interface State extends RegistrableEntity<StateType> {
  // MetaData management to the Designer
  StateType getType();
}
