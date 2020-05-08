package io.harness.engine.services;

import io.harness.ambiance.Ambiance;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;

import javax.validation.Valid;

public interface OutcomeService {
  OutcomeInstance save(@Valid OutcomeInstance outcomeInstance);

  Outcome findOutcome(Ambiance ambiance, String name);
}
