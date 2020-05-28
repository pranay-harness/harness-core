package io.harness.engine.interrupts;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.ExecutionInterruptType.planLevelInterrupts;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.engine.interrupts.handlers.PauseAllHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InterruptServiceImpl implements InterruptService {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private PauseAllHandler pauseAllHandler;

  @Override
  public InterruptCheck checkAndHandleInterruptsBeforeNodeStart(
      Ambiance ambiance, List<StepTransput> additionalInputs) {
    List<Interrupt> interrupts = fetchActivePlanLevelInterrupts(ambiance.getPlanExecutionId());
    if (isEmpty(interrupts)) {
      return InterruptCheck.builder().proceed(true).reason("[InterruptCheck] No Interrupts Found").build();
    }
    if (interrupts.size() > 1) {
      throw new InvalidRequestException("More than 2 active Plan Level Interrupts Present: "
          + interrupts.stream().map(interrupt -> interrupt.getType().toString()).collect(Collectors.joining("|")));
    }
    Interrupt interrupt = interrupts.get(0);

    switch (interrupt.getType()) {
      case PAUSE_ALL:
        pauseAllHandler.handleInterrupt(interrupt, ambiance, additionalInputs);
        return InterruptCheck.builder().proceed(false).reason("[InterruptCheck] PAUSE_ALL interrupt found").build();
      case RESUME_ALL:
        return InterruptCheck.builder().proceed(true).reason("[InterruptCheck] RESUME_ALL interrupt found").build();
      default:
        throw new InvalidRequestException("No Handler Present for interrupt type: " + interrupt.getType());
    }
  }

  @Override
  public List<Interrupt> fetchActivePlanLevelInterrupts(String planExecutionId) {
    List<Interrupt> interrupts = new ArrayList<>();
    Query<Interrupt> interruptQuery = hPersistence.createQuery(Interrupt.class, excludeAuthority)
                                          .filter(InterruptKeys.planExecutionId, planExecutionId)
                                          .filter(InterruptKeys.seized, Boolean.FALSE)
                                          .field(InterruptKeys.type)
                                          .in(planLevelInterrupts())
                                          .order(Sort.descending(InterruptKeys.createdAt));
    try (HIterator<Interrupt> interruptIterator = new HIterator<>(interruptQuery.fetch())) {
      while (interruptIterator.hasNext()) {
        interrupts.add(interruptIterator.next());
      }
    }
    return interrupts;
  }

  @Override
  public Interrupt seize(String interruptId) {
    UpdateOperations<Interrupt> updateOps =
        hPersistence.createUpdateOperations(Interrupt.class).set(InterruptKeys.seized, Boolean.TRUE);
    Query<Interrupt> interruptQuery = hPersistence.createQuery(Interrupt.class).filter(InterruptKeys.uuid, interruptId);
    Interrupt seizedInterrupt = hPersistence.findAndModify(interruptQuery, updateOps, HPersistence.returnNewOptions);
    if (seizedInterrupt == null) {
      throw new InvalidRequestException("Cannot seize the interrupt {} with id :" + interruptId);
    }
    return seizedInterrupt;
  }

  @Override
  public List<Interrupt> fetchActiveInterrupts(String planExecutionId) {
    List<Interrupt> interrupts = new ArrayList<>();
    Query<Interrupt> interruptQuery = hPersistence.createQuery(Interrupt.class, excludeAuthority)
                                          .filter(InterruptKeys.planExecutionId, planExecutionId)
                                          .filter(InterruptKeys.seized, Boolean.FALSE)
                                          .order(Sort.descending(InterruptKeys.createdAt));
    try (HIterator<Interrupt> interruptIterator = new HIterator<>(interruptQuery.fetch())) {
      while (interruptIterator.hasNext()) {
        interrupts.add(interruptIterator.next());
      }
    }
    return interrupts;
  }
}
