package io.harness.engine.services;

import io.harness.ambiance.Ambiance;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.resolvers.Resolver;

import javax.validation.Valid;

public interface OutcomeService extends Resolver {
  OutcomeInstance save(@Valid OutcomeInstance outcomeInstance);

  <T extends Outcome> T findOutcome(Ambiance ambiance, String name);
}
