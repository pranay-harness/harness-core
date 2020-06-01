package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.references.RefObject;
import io.harness.resolvers.Resolver;

import java.util.Optional;

@OwnedBy(CDC)
public interface OutcomeService extends Resolver<Outcome> {
  Optional<Outcome> resolve(Ambiance ambiance, RefObject refObject, String runtimeId);

  // TODO => Improve this evaluate if this is even needed
  <T extends Outcome> T findOutcome(Ambiance ambiance, String outcomeName);
}
