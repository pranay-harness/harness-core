package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFilename;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
import io.harness.queue.QueueController;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 */
@Slf4j
public class Notifier implements Runnable {
  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Queue<NotifyEvent> notifyQueue;
  @Inject private QueueController queueController;

  @Override
  public void run() {
    if (getMaintenanceFilename() || queueController.isNotPrimary()) {
      return;
    }

    try {
      execute();
    } catch (Exception e) {
      logger.error("Exception happened in Notifier execute", e);
    }
  }

  public void execute() {
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(Notifier.class, Notifier.class.getName(), Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }
      executeUnderLock();
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }

  public void executeUnderLock() {
    logger.debug("Execute Notifier response processing");

    // Sometimes response might arrive before we schedule the wait. Do not remove responses that are very new.
    final long limit = System.currentTimeMillis() - Duration.ofSeconds(15).toMillis();

    List<String> deleteResponses = new ArrayList<>();

    try (HIterator<NotifyResponse> iterator =
             new HIterator(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                               .project(NotifyResponseKeys.uuid, true)
                               .project(NotifyResponseKeys.createdAt, true)
                               .fetch())) {
      for (NotifyResponse notifyResponse : iterator) {
        final Query<WaitInstance> raceQuery =
            persistence.createQuery(WaitInstance.class, excludeAuthority)
                .filter(WaitInstanceKeys.waitingOnCorrelationIds, notifyResponse.getUuid());

        final UpdateOperations<WaitInstance> raceOperations =
            persistence.createUpdateOperations(WaitInstance.class)
                .removeAll(WaitInstanceKeys.waitingOnCorrelationIds, notifyResponse.getUuid());

        persistence.update(raceQuery, raceOperations);

        try (HIterator<WaitInstance> waitInstances =
                 new HIterator(persistence.createQuery(WaitInstance.class, excludeAuthority)
                                   .filter(WaitInstanceKeys.correlationIds, notifyResponse.getUuid())
                                   .filter(WaitInstanceKeys.status, ExecutionStatus.NEW)
                                   .fetch())) {
          if (notifyResponse.getCreatedAt() < limit && !waitInstances.hasNext()) {
            deleteResponses.add(notifyResponse.getUuid());
            if (deleteResponses.size() >= 1000) {
              deleteObsoleteResponses(deleteResponses);
              deleteResponses.clear();
            }
          }

          for (WaitInstance waitInstance : waitInstances) {
            if (isEmpty(waitInstance.getWaitingOnCorrelationIds())) {
              notifyQueue.send(aNotifyEvent().waitInstanceId(waitInstance.getUuid()).build());
            }
          }
        }
      }

      deleteObsoleteResponses(deleteResponses);
    }
  }

  private void deleteObsoleteResponses(List<String> deleteResponses) {
    if (isNotEmpty(deleteResponses)) {
      logger.info("Deleting {} not needed responses", deleteResponses.size());
      persistence.delete(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                             .field(NotifyResponseKeys.uuid)
                             .in(deleteResponses));
    }
  }
}
