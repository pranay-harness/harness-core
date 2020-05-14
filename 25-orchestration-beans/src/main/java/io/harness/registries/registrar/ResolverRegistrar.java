package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefType;
import io.harness.resolvers.Resolver;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface ResolverRegistrar extends EngineRegistrar<RefType, Resolver<?>> {
  void register(Set<Pair<RefType, Class<? extends Resolver<?>>>> resolverClasses);
}
