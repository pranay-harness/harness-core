package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.resolvers.Resolver;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface OutcomeService extends Resolver<Outcome> {
  Optional<Outcome> find(
      @NotNull Ambiance ambiance, @NotNull String setupId, @NotNull String runtimeId, @NotNull String name);
}
