package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFilename;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeAuthorityCount;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.lang.Math.max;
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
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 */
@Slf4j
public class Notifier implements Runnable {
  static final int PAGE_SIZE = 2000;

  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Queue<NotifyEvent> notifyQueue;
  @Inject private QueueController queueController;

  private int step = PAGE_SIZE / 3 + new Random().nextInt(PAGE_SIZE / 3);
  private int skip;
  private boolean alternateSkip;

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
    final List<NotifyResponse> notifyResponses =
        persistence.createQuery(NotifyResponse.class, excludeAuthorityCount)
            .project(NotifyResponseKeys.uuid, true)
            .project(NotifyResponseKeys.createdAt, true)
            .asList(new FindOptions().skip(alternateSkip ? skip : 0).limit(PAGE_SIZE));

    if (isEmpty(notifyResponses)) {
      logger.debug("There are no NotifyResponse entries to process");
      skip = 0;
      return;
    }

    logger.info("Notifier responses {} with skip {}", notifyResponses.size(), skip);

    if (alternateSkip) {
      skip = nextSkip(skip, notifyResponses.size(), step,
          () -> (int) persistence.createQuery(NotifyResponse.class, excludeAuthority).count());
    }
    alternateSkip = !alternateSkip;

    Set<String> correlationIds = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toSet());
    Map<String, List<String>> waitInstances = new HashMap<>();

    // Get wait queue entries
    try (
        HIterator<WaitInstance> iterator = new HIterator<>(persistence.createQuery(WaitInstance.class, excludeAuthority)
                                                               .field(WaitInstanceKeys.correlationIds)
                                                               .in(correlationIds)
                                                               .fetch())) {
      for (WaitInstance waitInstance : iterator) {
        if (correlationIds.containsAll(waitInstance.getCorrelationIds())) {
          final String waitInstanceId = waitInstance.getUuid();
          waitInstances.computeIfAbsent(waitInstanceId, key -> new ArrayList<String>())
              .addAll(waitInstance.getCorrelationIds());
        }
        correlationIds.removeAll(waitInstance.getCorrelationIds());
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
        logger.warn("Deleting zombie responses {}", deleteResponses.toString());
        persistence.delete(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                               .field(NotifyResponseKeys.uuid)
                               .in(deleteResponses));
      }
    }
  }

  static int nextSkip(int current, int loaded, int step, IntSupplier collectionSize) {
    int next = current;
    if (current == 0) {
      if (loaded >= PAGE_SIZE) {
        next = collectionSize.getAsInt() - PAGE_SIZE;
      }
    } else {
      next -= step;
    }

    return max(0, next);
  }
}
