package io.harness.pms.sdk.core.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface ResolverRegistrar extends Registrar<RefType, Resolver<?>> {
  void register(Set<Pair<RefType, Resolver<?>>> resolverClasses);
}
