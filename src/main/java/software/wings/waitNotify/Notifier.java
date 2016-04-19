package software.wings.waitNotify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static software.wings.waitNotify.NotifyEvent.Builder.aNotifyEvent;
import static org.eclipse.jetty.util.LazyList.isEmpty;

/**
 *
 *
 * @author Rishi
 *
 */
public class Notifier implements Runnable {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  @Inject private Queue<NotifyEvent> notifyQueue;

  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(Notifier.class, Notifier.class.getName());
      if (!lockAcquired) {
        log().warn("Persistent lock could not be acquired for the Notifier");
        return;
      }
      PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
      reqNotifyRes.getFieldsIncluded().add("uuid");
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);

      if (notifyPageResponses == null || isEmpty(notifyPageResponses.getResponse())) {
        log().debug("There are no NotifyResponse entries to process");
      }

      List<NotifyResponse> notifyResponses = notifyPageResponses.getResponse();
      List<String> correlationIds = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      PageRequest<WaitQueue> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");
      filter.setFieldValues(correlationIds);
      filter.setOp(OP.IN);
      req.getFilters().add(filter);

      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);

      if (waitQueuesResponse == null || isEmpty(waitQueuesResponse.getResponse())) {
        log().warn("No entry in the waitQueue found for the correlationIds: {} skipping ...", correlationIds);
        return;
      }

      List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
      // process distinct set of wait instanceIds
      waitQueues.stream()
          .map(WaitQueue::getWaitInstanceId)
          .forEach(waitInstanceId
              -> notifyQueue.send(
                  aNotifyEvent().withWaitInstanceId(waitInstanceId).withCorrelationIds(correlationIds).build()));

    } catch (Exception e) {
      log().error("Error seen in the Notifier call", e);
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
