package io.harness.engine.interrupts;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InterruptServiceImpl implements InterruptService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public boolean seize(String interruptId) {
    UpdateOperations<Interrupt> updateOps =
        hPersistence.createUpdateOperations(Interrupt.class).set(InterruptKeys.seized, Boolean.TRUE);

    Query<Interrupt> interruptQuery = hPersistence.createQuery(Interrupt.class).filter(InterruptKeys.uuid, interruptId);
    Interrupt seizedInterrupt = hPersistence.findAndModify(interruptQuery, updateOps, HPersistence.returnNewOptions);
    return seizedInterrupt != null;
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
