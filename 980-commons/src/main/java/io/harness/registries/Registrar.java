package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface Registrar<K, T> {
  void register(Set<Pair<K, T>> registrableEntities);
}
