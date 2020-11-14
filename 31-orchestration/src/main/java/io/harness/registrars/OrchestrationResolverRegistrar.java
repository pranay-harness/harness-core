package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.references.RefType;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.resolvers.Resolver;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationResolverRegistrar implements ResolverRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<RefType, Resolver<?>>> resolverClasses) {
    resolverClasses.add(
        Pair.of(RefType.builder().type(RefType.OUTCOME).build(), injector.getInstance(OutcomeService.class)));
    resolverClasses.add(Pair.of(RefType.builder().type(RefType.SWEEPING_OUTPUT).build(),
        injector.getInstance(ExecutionSweepingOutputService.class)));
  }
}
