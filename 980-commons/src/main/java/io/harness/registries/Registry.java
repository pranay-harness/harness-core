package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface Registry<K, V> {
  void register(K registryKey, V registrableEntity);

  V obtain(K k);

  String getType();
}
