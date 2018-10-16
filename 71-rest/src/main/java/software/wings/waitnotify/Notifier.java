package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HIterator;
import io.harness.queue.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class Notifier implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Queue<NotifyEvent> notifyQueue;
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance() || configurationController.isNotPrimary()) {
      return;
    }

    try {
      execute();
    } catch (Exception e) {
      logger.error("Exception happened in Notifier execute", e);
    }
  }

  public void execute() {
    logger.info("Execute Notifier response processing");
    final List<NotifyResponse> notifyResponses = wingsPersistence.createQuery(NotifyResponse.class, excludeAuthority)
                                                     .project(NotifyResponse.ID_KEY, true)
                                                     .project(NotifyResponse.CREATED_AT_KEY, true)
                                                     .asList();

    if (isEmpty(notifyResponses)) {
      logger.debug("There are no NotifyResponse entries to process");
      return;
    }

    logger.info("Notifier responses {}", notifyResponses.size());

    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(Notifier.class, Notifier.class.getName(), Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }

      List<String> correlationIds = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      try (HIterator<WaitQueue> iterator = new HIterator<WaitQueue>(wingsPersistence.createQuery(WaitQueue.class)
                                                                        .field(WaitQueue.CORRELATION_ID_KEY)
                                                                        .in(correlationIds)
                                                                        .fetch())) {
        if (!iterator.hasNext()) {
          // All responses that are older than a minute and do not have wait queue yet should be considered zombies.
          // We would like to delete them to avoid blocking the collection.
          final long limit = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();
          List<String> deleteResponses = notifyResponses.stream()
                                             .filter(response -> response.getCreatedAt() < limit)
                                             .map(NotifyResponse::getUuid)
                                             .collect(toList());

          if (isNotEmpty(deleteResponses)) {
            logger.warn("Deleting zombie responses {}", correlationIds.toString());
            wingsPersistence.delete(wingsPersistence.createQuery(NotifyResponse.class, excludeAuthority)
                                        .field(NotifyResponse.ID_KEY)
                                        .in(deleteResponses));
          }

          if (correlationIds.size() > 750) {
            logger.error("No entry in the waitQueue found for dangerously big number {} of correlationIds",
                correlationIds.size());
          } else if (correlationIds.size() > 200) {
            logger.warn("No entry in the waitQueue found for {} correlationIds", correlationIds.size());
          }
        }

        Set<String> handled = new HashSet<>();

        while (iterator.hasNext()) {
          // process distinct set of wait instanceIds
          final WaitQueue waitQueue = iterator.next();
          final String waitInstanceId = waitQueue.getWaitInstanceId();
          if (handled.contains(waitInstanceId)) {
            continue;
          }
          handled.add(waitInstanceId);

          notifyQueue.send(
              aNotifyEvent().withWaitInstanceId(waitInstanceId).withCorrelationIds(correlationIds).build());
        }
      } catch (WingsException exception) {
        WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception exception) {
        logger.error("Error seen in the Notifier call", exception);
      }
    }
  }
}
