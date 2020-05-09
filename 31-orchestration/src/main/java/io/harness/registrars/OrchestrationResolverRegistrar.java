package io.harness.registrars;

import io.harness.engine.services.OutcomeService;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.resolvers.Resolver;

import java.util.Set;

public class OrchestrationResolverRegistrar implements ResolverRegistrar {
  @Override
  public void register(Set<Class<? extends Resolver>> resolverClasses) {
    resolverClasses.add(OutcomeService.class);
  }
}
