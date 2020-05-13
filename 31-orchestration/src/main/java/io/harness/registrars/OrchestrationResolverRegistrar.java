package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.OutcomeService;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputResolver;
import io.harness.resolvers.Resolver;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationResolverRegistrar implements ResolverRegistrar {
  @Override
  public void register(Set<Class<? extends Resolver>> resolverClasses) {
    resolverClasses.add(OutcomeService.class);
    resolverClasses.add(ExecutionSweepingOutputResolver.class);
  }
}
