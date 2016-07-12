package software.wings.waitnotify;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.core.queue.Queue;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;

import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class Notifier implements Runnable {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  @Inject private Queue<NotifyEvent> notifyQueue;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(Notifier.class, Notifier.class.getName());
      if (!lockAcquired) {
        log().warn("Persistent lock could not be acquired for the Notifier");
        return;
      }

      PageResponse<NotifyResponse> notifyPageResponses =
          wingsPersistence.query(NotifyResponse.class, aPageRequest().addFieldsIncluded(ID_KEY).build());

      if (isEmpty(notifyPageResponses)) {
        log().debug("There are no NotifyResponse entries to process");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class,
          aPageRequest()
              .addFilter(aSearchFilter().withField("correlationId", Operator.IN, correlationIds.toArray()).build())
              .build());

      if (isEmpty(waitQueuesResponse)) {
        log().warn("No entry in the waitQueue found for the correlationIds: {} skipping ...", correlationIds);
        return;
      }

      // process distinct set of wait instanceIds
      waitQueuesResponse.stream()
          .map(WaitQueue::getWaitInstanceId)
          .forEach(waitInstanceId
              -> notifyQueue.send(
                  aNotifyEvent().withWaitInstanceId(waitInstanceId).withCorrelationIds(correlationIds).build()));

    } catch (Exception exception) {
      log().error("Error seen in the Notifier call", exception);
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(Notifier.class, Notifier.class.getName());
      }
    }
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
