package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resolvers.Resolver;

import java.util.Set;

@OwnedBy(CDC)
public interface ResolverRegistrar extends EngineRegistrar<Resolver> {
  void register(Set<Class<? extends Resolver>> resolverClasses);
}
