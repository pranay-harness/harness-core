package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface Registry<K extends RegistryKey, V extends Class<? extends RegistrableEntity>> {
  void register(K registryKey, V registrableEntity);

  RegistrableEntity obtain(K k);

  String getType();
}
