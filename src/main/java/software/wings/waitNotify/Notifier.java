package software.wings.waitNotify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.common.thread.ThreadPool;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;
import software.wings.utils.CollectionUtils;

/**
 *
 *
 * @author Rishi
 *
 */
public class Notifier implements Runnable {
  private WingsPersistence wingsPersistence;
  private PersistentLocker persistentLocker;
  private ExecutorService executorService;

  public Notifier(
      WingsPersistence wingsPersistence, PersistentLocker persistentLocker, ExecutorService executorService) {
    this.wingsPersistence = wingsPersistence;
    this.persistentLocker = persistentLocker;
    this.executorService = executorService;
  }

  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(Notifier.class, Notifier.class.getName());
      if (!lockAcquired) {
        logger.warn("Persistent lock could not be acquired for the Notifier");
        return;
      }
      PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
      reqNotifyRes.getFieldsIncluded().add("uuid");
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
      if (notifyPageResponses == null || notifyPageResponses.getResponse() == null) {
        logger.debug("There are no NotifyResponse entries to process");
      }
      List<NotifyResponse> notifyResponses = notifyPageResponses.getResponse();
      List<String> correlationIds = CollectionUtils.fields(String.class, notifyResponses, "uuid");

      // Get wait queue entries
      PageRequest<WaitQueue> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");
      filter.setFieldValues(correlationIds);
      filter.setOp(OP.IN);
      req.getFilters().add(filter);
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
      if (waitQueuesResponse == null || waitQueuesResponse.getResponse() == null
          || waitQueuesResponse.getResponse().size() == 0) {
        logger.warn("No entry in the waitQueue found for the correlationIds:" + correlationIds + " skipping ...");
        return;
      }
      List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
      // process distinct set of wait instanceIds
      Set<String> waitInstanceIds = new HashSet<>();
      for (WaitQueue waitQueue : waitQueues) {
        waitInstanceIds.add(waitQueue.getWaitInstanceId());
      }
      for (String waitInstanceId : waitInstanceIds) {
        executorService.execute(
            new NotifierForWaitInstance(wingsPersistence, persistentLocker, waitInstanceId, correlationIds));
      }
    } catch (Exception e) {
      logger.error("Error seen in the Notifier call", e);
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(Notifier.class, Notifier.class.getName());
      }
    }
  }
  private static Logger logger = LoggerFactory.getLogger(Notifier.class);
}
