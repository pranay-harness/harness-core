package software.wings.waitNotify;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;

/**
 * @author Rishi
 *
 */
public class NotifierForWaitInstance implements Runnable {
  private String waitInstanceId;
  private List<String> correlationIds;
  private WingsPersistence wingsPersistence;
  private PersistentLocker persistentLocker;

  public NotifierForWaitInstance(
      WingsPersistence wingsPersistence, String waitInstanceId, List<String> correlationIds) {
    this.wingsPersistence = wingsPersistence;
    this.waitInstanceId = waitInstanceId;
    this.correlationIds = correlationIds;
    PersistentLocker.init(wingsPersistence);
    persistentLocker = PersistentLocker.getInstance();
  }

  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(WaitInstance.class, waitInstanceId);
      if (!lockAcquired) {
        logger.warn("Persistent lock could not be acquired for the waitInstanceId: " + waitInstanceId);
      }
      PageRequest<WaitQueue> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("waitInstanceId");
      filter.setFieldValue(waitInstanceId);
      filter.setOp(OP.EQ);
      req.getFilters().add(filter);
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
      if (waitQueuesResponse == null || waitQueuesResponse.getResponse() == null
          || waitQueuesResponse.getResponse().size() == 0) {
        logger.warn("No entry in the waitQueue found for the waitInstanceId:" + waitInstanceId + " skipping ...");
        return;
      }
      List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
      for (WaitQueue waitQueue : waitQueues) {
        if (!correlationIds.contains(waitQueue.getCorrelationId())) {
          logger.warn(
              "Some of the correlationIds still needs to be waited ..correlationId:" + waitQueue.getCorrelationId());
          return;
        }
      }

      WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId);
      if (waitInstance == null) {
        logger.warn("waitInstance not found for waitInstanceId:" + waitInstanceId);
        return;
      }
      Map<String, Serializable> responseMap = new HashMap<>();
      for (WaitQueue waitQueue : waitQueues) {
        NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, waitQueue.getCorrelationId());
        if (notifyResponse == null) {
          logger.error("notifyResponse for the correlationId:" + waitQueue.getCorrelationId()
              + " not found. skipping the callback for the waitInstanceId :" + waitInstanceId);
          return;
        }
        responseMap.put(waitQueue.getCorrelationId(), notifyResponse.getResponse());
      }

      NotifyCallback callback = waitInstance.getCallback();
      if (callback != null) {
        try {
          callback.notify(responseMap);
        } catch (Exception e) {
          logger.error("WaitInstance callback failed - waitInstanceId:" + waitInstanceId, e);
        }
      }

      // time to cleanup
      for (WaitQueue waitQueue : waitQueues) {
        try {
          wingsPersistence.delete(NotifyResponse.class, waitQueue.getCorrelationId());
          wingsPersistence.delete(waitQueue);
        } catch (Exception e) {
          // ignore
        }
      }
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(WaitInstance.class, waitInstanceId);
      }
    }
  }

  private static Logger logger = LoggerFactory.getLogger(Notifier.class);
}
