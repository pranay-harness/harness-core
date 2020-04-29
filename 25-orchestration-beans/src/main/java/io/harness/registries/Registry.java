package io.harness.registries;

import io.harness.annotations.Redesign;

@Redesign
public interface Registry<K extends RegistryKey, V extends RegistrableEntity<K>> {
  void register(K registryKey, V registrableEntity);

  V obtain(K k);

  RegistryType getType();

  Class<V> getRegistrableEntityClass();
}
