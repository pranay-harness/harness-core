package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.registries.registrar.ResolverRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class OrchestrationResolverRegistrar implements ResolverRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<RefType, Resolver<?>>> resolverClasses) {
    resolverClasses.add(Pair.of(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build(),
        injector.getInstance(OutcomeService.class)));
    resolverClasses.add(Pair.of(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build(),
        injector.getInstance(ExecutionSweepingOutputService.class)));
  }
}
