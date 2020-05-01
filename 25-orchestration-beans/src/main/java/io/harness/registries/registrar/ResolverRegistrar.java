package io.harness.registries.registrar;

import io.harness.resolvers.Resolver;

import java.util.Set;

public interface ResolverRegistrar extends EngineRegistrar<Resolver> {
  void register(Set<Class<? extends Resolver>> resolverClasses);
}
