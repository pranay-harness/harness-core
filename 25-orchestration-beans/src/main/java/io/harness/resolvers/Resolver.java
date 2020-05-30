package io.harness.resolvers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefObject;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StepTransput;

@OwnedBy(CDC)
@Redesign
public interface Resolver<T extends StepTransput> extends RegistrableEntity {
  T resolve(Ambiance ambiance, RefObject refObject);

  T consume(Ambiance ambiance, String name, T value);
}
