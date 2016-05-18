package software.wings.waitnotify;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
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

      if (isEmpty(notifyPageResponses)) {
        log().debug("There are no NotifyResponse entries to process");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");

      ArrayList<Object> fieldValues = new ArrayList();
      fieldValues.addAll(correlationIds);
      filter.setFieldValues(fieldValues);
      filter.setOp(Operator.IN);
      PageRequest<WaitQueue> req = new PageRequest<>();
      req.getFilters().add(filter);

      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);

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
