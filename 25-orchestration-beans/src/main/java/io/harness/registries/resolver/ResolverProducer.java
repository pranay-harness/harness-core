package io.harness.registries.resolver;

import io.harness.refrences.RefType;
import io.harness.resolvers.Resolver;

public interface ResolverProducer {
  Resolver produceResolver(RefType refType);

  RefType getType();
}
