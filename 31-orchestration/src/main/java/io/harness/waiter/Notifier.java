package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
import io.harness.queue.QueueController;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import org.mongodb.morphia.query.FindOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class Notifier implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Queue<NotifyEvent> notifyQueue;
  @Inject private QueueController queueController;

  @Override
  public void run() {
    if (isMaintenance() || queueController.isNotPrimary()) {
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
    final List<NotifyResponse> notifyResponses = persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                                     .project(NotifyResponseKeys.uuid, true)
                                                     .project(NotifyResponseKeys.createdAt, true)
                                                     .asList(new FindOptions().limit(1000));

    if (isEmpty(notifyResponses)) {
      logger.debug("There are no NotifyResponse entries to process");
      return;
    }

    logger.info("Notifier responses {}", notifyResponses.size());

    Set<String> correlationIds = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toSet());
    Map<String, List<String>> waitInstances = new HashMap<>();

    // Get wait queue entries
    try (HIterator<WaitQueue> iterator =
             new HIterator<WaitQueue>(persistence.createQuery(WaitQueue.class, excludeAuthority)
                                          .field(WaitQueue.CORRELATION_ID_KEY)
                                          .in(correlationIds)
                                          .fetch())) {
      while (iterator.hasNext()) {
        // process distinct set of wait instanceIds
        final WaitQueue waitQueue = iterator.next();
        final String waitInstanceId = waitQueue.getWaitInstanceId();
        waitInstances.computeIfAbsent(waitInstanceId, key -> new ArrayList<String>()).add(waitQueue.getCorrelationId());
        correlationIds.remove(waitQueue.getCorrelationId());
      }
    }

    waitInstances.forEach((waitInstanceId, correlationIdList) -> {
      notifyQueue.send(aNotifyEvent().waitInstanceId(waitInstanceId).correlationIds(correlationIdList).build());
      logger.info("Send notification for waitInstanceId: {}", waitInstanceId);
    });

    // All responses that are older and do not have wait queue yet should be considered zombies.
    // We would like to delete them to avoid blocking the collection.
    if (isNotEmpty(correlationIds)) {
      final long limit = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();
      List<String> deleteResponses = notifyResponses.stream()
                                         .filter(response -> response.getCreatedAt() < limit)
                                         .filter(response -> correlationIds.contains(response.getUuid()))
                                         .map(NotifyResponse::getUuid)
                                         .collect(toList());

      if (isNotEmpty(deleteResponses)) {
        logger.warn("Deleting zombie responses {}", correlationIds.toString());
        persistence.delete(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                               .field(NotifyResponseKeys.uuid)
                               .in(deleteResponses));
      }
    }
  }
}
