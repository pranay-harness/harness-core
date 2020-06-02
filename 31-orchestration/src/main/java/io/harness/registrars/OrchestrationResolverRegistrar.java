package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.OutcomeService;
import io.harness.references.RefType;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.resolvers.Resolver;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationResolverRegistrar implements ResolverRegistrar {
  @Override
  public void register(Set<Pair<RefType, Class<? extends Resolver<?>>>> resolverClasses) {
    resolverClasses.add(Pair.of(RefType.builder().type(RefType.OUTCOME).build(), OutcomeService.class));
    resolverClasses.add(
        Pair.of(RefType.builder().type(RefType.SWEEPING_OUTPUT).build(), ExecutionSweepingOutputService.class));
  }
}
