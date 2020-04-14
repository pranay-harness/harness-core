package io.harness.registries;

import io.harness.annotations.Redesign;

@Redesign
public interface Registry {
  RegistryType getType();
}
