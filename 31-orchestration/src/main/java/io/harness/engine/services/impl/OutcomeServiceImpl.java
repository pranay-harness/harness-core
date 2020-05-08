package io.harness.engine.services.impl;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.engine.services.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

public class OutcomeServiceImpl implements OutcomeService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public Outcome findOutcome(Ambiance ambiance, String name) {
    OutcomeInstance outcomeInstance = hPersistence.createQuery(OutcomeInstance.class)
                                          .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                          .filter(OutcomeInstanceKeys.name, name)
                                          .get();
    if (outcomeInstance == null) {
      return null;
    }
    return outcomeInstance.getOutcome();
  }

  @Override
  public OutcomeInstance save(OutcomeInstance outcomeInstance) {
    try {
      hPersistence.save(outcomeInstance);
      return outcomeInstance;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException(
          format("Outcome with name %s, already saved", outcomeInstance.getName()), exception);
    }
  }
}
