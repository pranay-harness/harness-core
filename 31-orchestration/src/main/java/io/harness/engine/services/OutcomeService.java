package io.harness.engine.services;

import io.harness.ambiance.Ambiance;
import io.harness.data.Outcome;
import io.harness.resolvers.Resolver;

public interface OutcomeService extends Resolver<Outcome> {
  // TODO => Improve this evaluate if this is even needed
  <T extends Outcome> T findOutcome(Ambiance ambiance, String outcomeName);
}
