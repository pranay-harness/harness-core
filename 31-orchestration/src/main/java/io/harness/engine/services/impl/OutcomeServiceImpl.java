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
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.state.io.StateTransput;

public class OutcomeServiceImpl implements OutcomeService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public <T extends Outcome> T findOutcome(Ambiance ambiance, String name) {
    OutcomeInstance outcomeInstance = hPersistence.createQuery(OutcomeInstance.class)
                                          .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                          .filter(OutcomeInstanceKeys.name, name)
                                          .get();
    if (outcomeInstance == null) {
      return null;
    }
    return (T) outcomeInstance.getOutcome();
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

  @Override
  public <T extends StateTransput> T resolve(Ambiance ambiance, RefObject refObject) {
    OutcomeInstance outcomeInstance = hPersistence.createQuery(OutcomeInstance.class)
                                          .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                          .filter(OutcomeInstanceKeys.levelExecutionSetupId, refObject.getProducerId())
                                          .get();
    if (outcomeInstance == null) {
      return null;
    }
    return (T) outcomeInstance.getOutcome();
  }

  @Override
  public RefType getType() {
    return RefType.builder().type("OUTCOME").build();
  }
}
