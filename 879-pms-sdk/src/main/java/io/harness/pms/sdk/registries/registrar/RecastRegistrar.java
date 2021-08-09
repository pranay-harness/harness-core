package io.harness.pms.sdk.registries.registrar;

import java.util.Set;

public interface RecastRegistrar {
  void register(Set<Class<?>> classes);
}
