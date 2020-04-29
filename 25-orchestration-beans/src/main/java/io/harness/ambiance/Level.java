package io.harness.ambiance;

import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;

@Redesign
public interface Level extends RegistrableEntity<LevelType> {
  LevelType getType();
  int getOrder();
}
