package io.harness.resolvers;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StateTransput;

@Redesign
public interface Resolver<T extends StateTransput> extends RegistrableEntity<RefType> {
  T resolve(Ambiance ambiance, RefObject refObject);

  T consume(Ambiance ambiance, String name, T value);

  RefType getType();

  default RefType getRegistryKey() {
    return getType();
  }
}
