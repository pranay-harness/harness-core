package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.core.queue.Queue;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;

import java.time.Duration;
import java.util.List;

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

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance()) {
      return;
    }

    try (AcquiredLock lock =
             persistentLocker.acquireLock(Notifier.class, Notifier.class.getName(), Duration.ofMinutes(1))) {
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(
          NotifyResponse.class, aPageRequest().withLimit(UNLIMITED).addFieldsIncluded(ID_KEY).build(), false, true);

      if (isEmpty(notifyPageResponses)) {
        logger.debug("There are no NotifyResponse entries to process");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class,
          aPageRequest().withLimit(UNLIMITED).addFilter("correlationId", Operator.IN, correlationIds.toArray()).build(),
          false, true);

      if (isEmpty(waitQueuesResponse)) {
        if (correlationIds.size() > 200) {
          logger.warn("No entry in the waitQueue found for {} correlationIds", correlationIds.size());
        } else if (correlationIds.size() > 750) {
          logger.error(
              "No entry in the waitQueue found for dangerously big number {} of correlationIds", correlationIds.size());
        }
        return;
      }

      // process distinct set of wait instanceIds
      waitQueuesResponse.stream()
          .map(WaitQueue::getWaitInstanceId)
          .distinct()
          .forEach(waitInstanceId
              -> notifyQueue.send(
                  aNotifyEvent().withWaitInstanceId(waitInstanceId).withCorrelationIds(correlationIds).build()));
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }
}
