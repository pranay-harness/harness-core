package io.harness.resolvers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StateTransput;

@OwnedBy(CDC)
@Redesign
public interface Resolver<T extends StateTransput> extends RegistrableEntity<RefType> {
  T resolve(Ambiance ambiance, RefObject refObject);

  T consume(Ambiance ambiance, String name, T value);

  RefType getType();

  default RefType getRegistryKey() {
    return getType();
  }
}
