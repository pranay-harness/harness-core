package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface RegistryKey {
  Object getType();
}
