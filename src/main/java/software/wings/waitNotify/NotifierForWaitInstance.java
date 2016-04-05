package software.wings.waitNotify;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;
import software.wings.sm.ExecutionStatus;

/**
 * @author Rishi
 *
 */
public class NotifierForWaitInstance implements Runnable {
  private String waitInstanceId;
  private List<String> correlationIds;
  private WingsPersistence wingsPersistence;
  private PersistentLocker persistentLocker;

  public NotifierForWaitInstance(WingsPersistence wingsPersistence, PersistentLocker persistentLocker,
      String waitInstanceId, List<String> correlationIds) {
    this.wingsPersistence = wingsPersistence;
    this.waitInstanceId = waitInstanceId;
    this.correlationIds = correlationIds;
    this.persistentLocker = persistentLocker;
  }

  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(WaitInstance.class, waitInstanceId);
      if (!lockAcquired) {
        logger.warn("Persistent lock could not be acquired for the waitInstanceId: " + waitInstanceId);
        return;
      }

      WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId);
      if (waitInstance == null) {
        logger.warn("waitInstance not found for waitInstanceId:" + waitInstanceId);
        return;
      }
      if (waitInstance.getStatus() != ExecutionStatus.NEW) {
        logger.warn("WaitInstance already processed - waitInstanceId:" + waitInstanceId
            + ", status=" + waitInstance.getStatus() + " skipping ...");
        return;
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

      Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();

      Map<String, Serializable> responseMap = new HashMap<>();
      for (WaitQueue waitQueue : waitQueues) {
        NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, waitQueue.getCorrelationId());
        if (notifyResponse == null) {
          logger.error("notifyResponse for the correlationId:" + waitQueue.getCorrelationId()
              + " not found. skipping the callback for the waitInstanceId :" + waitInstanceId);
          return;
        }
        responseMap.put(waitQueue.getCorrelationId(), notifyResponse.getResponse());
        notifyResponseMap.put(waitQueue.getCorrelationId(), notifyResponse);
      }

      ExecutionStatus status = ExecutionStatus.SUCCESS;
      NotifyCallback callback = waitInstance.getCallback();
      if (callback != null) {
        try {
          callback.notify(responseMap);
        } catch (Exception e) {
          status = ExecutionStatus.ERROR;
          logger.error("WaitInstance callback failed - waitInstanceId:" + waitInstanceId, e);
          try {
            WaitInstanceError waitInstanceError = new WaitInstanceError();
            waitInstanceError.setWaitInstanceId(waitInstanceId);
            waitInstanceError.setResponseMap(responseMap);
            waitInstanceError.setErrorStackTrace(ExceptionUtils.getStackTrace(e));

            wingsPersistence.save(waitInstanceError);
          } catch (Exception e2) {
            logger.error("Error in persisting waitInstanceError", e2);
          }
        }
      }

      // time to cleanup
      try {
        UpdateOperations<WaitInstance> waitInstanceUpdate =
            wingsPersistence.createUpdateOperations(WaitInstance.class).set("status", status);
        wingsPersistence.update(waitInstance, waitInstanceUpdate);
      } catch (Exception e) {
        logger.error("Error in waitInstanceUpdate", e);
      }

      UpdateOperations<NotifyResponse> notifyResponseUpdate =
          wingsPersistence.createUpdateOperations(NotifyResponse.class).set("status", ExecutionStatus.SUCCESS);
      for (WaitQueue waitQueue : waitQueues) {
        try {
          wingsPersistence.delete(waitQueue);
          wingsPersistence.update(notifyResponseMap.get(waitQueue.getCorrelationId()), notifyResponseUpdate);
        } catch (Exception e) {
          logger.error("Error in waitQueue cleanup", e);
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
