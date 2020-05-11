package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(CDC)
@Redesign
public interface Level extends RegistrableEntity<LevelType> {
  LevelType getType();
  int getOrder();
}
