package io.harness.registries.resolver;

import io.harness.annotations.Redesign;
import io.harness.refrences.RefType;
import io.harness.resolvers.Resolver;

@Redesign
public interface ResolverProducer {
  Resolver produceResolver(RefType refType);

  RefType getType();
}
