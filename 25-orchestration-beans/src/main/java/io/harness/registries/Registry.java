package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public interface Registry<K extends RegistryKey, V extends RegistrableEntity<K>> {
  void register(K registryKey, V registrableEntity);

  RegistrableEntity<K> obtain(K k);

  RegistryType getType();
}
