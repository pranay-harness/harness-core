package software.wings.waitnotify;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
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
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance() || configurationController.isNotPrimary()) {
      return;
    }
    execute();
  }

  public void execute() {
    PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(
        NotifyResponse.class, aPageRequest().withLimit(UNLIMITED).addFieldsIncluded(ID_KEY).build(), excludeAuthority);

    if (isEmpty(notifyPageResponses)) {
      logger.debug("There are no NotifyResponse entries to process");
      return;
    }

    try (AcquiredLock lock =
             persistentLocker.acquireLock(Notifier.class, Notifier.class.getName(), Duration.ofMinutes(1))) {
      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class,
          aPageRequest().withLimit(UNLIMITED).addFilter("correlationId", Operator.IN, correlationIds).build(),
          excludeAuthority);

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
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }
}
