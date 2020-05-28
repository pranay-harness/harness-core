package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.engine.services.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.references.RefObject;

@OwnedBy(CDC)
public class OutcomeServiceImpl implements OutcomeService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    OutcomeInstance outcomeInstance = hPersistence.createQuery(OutcomeInstance.class)
                                          .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                          .filter(OutcomeInstanceKeys.levelSetupId, refObject.getProducerId())
                                          .filter(OutcomeInstanceKeys.name, refObject.getName())
                                          .get();
    if (outcomeInstance == null) {
      return null;
    }
    return outcomeInstance.getOutcome();
  }

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
  public Outcome consume(Ambiance ambiance, String name, Outcome outcome) {
    OutcomeInstance instance = OutcomeInstance.builder()
                                   .uuid(generateUuid())
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levels(ambiance.getLevels())
                                   .outcome(outcome)
                                   .createdAt(System.currentTimeMillis())
                                   .name(name)
                                   .build();
    OutcomeInstance savedInstance = save(instance);
    if (savedInstance == null) {
      return null;
    }
    return savedInstance.getOutcome();
  }

  private OutcomeInstance save(OutcomeInstance outcomeInstance) {
    try {
      hPersistence.save(outcomeInstance);
      return outcomeInstance;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException(
          format("Outcome with name %s, already saved", outcomeInstance.getName()), exception);
    }
  }
}
