package io.harness.registries;

import io.harness.annotations.Redesign;

@Redesign
public interface Registry<K extends RegistryKey, V extends Class<? extends RegistrableEntity<K>>> {
  void register(K registryKey, V registrableEntity);

  RegistrableEntity<K> obtain(K k);

  RegistryType getType();
}
