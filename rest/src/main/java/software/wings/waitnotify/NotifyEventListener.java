package software.wings.waitnotify;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Injector;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;
import software.wings.sm.ExecutionStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Singleton
public final class NotifyEventListener extends AbstractQueueListener<NotifyEvent> {
  private static final Logger logger = LoggerFactory.getLogger(NotifyEventListener.class);

  @Inject private Injector injector;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onMessage(NotifyEvent message) throws Exception {
    logger.trace("Processing message {}", message);
    String waitInstanceId = message.getWaitInstanceId();

    WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId, ReadPref.CRITICAL);

    if (waitInstance == null) {
      logger.warn("waitInstance not found for waitInstanceId: {}", waitInstanceId);
      return;
    }
    if (waitInstance.getStatus() != ExecutionStatus.NEW) {
      logger.warn("WaitInstance already processed - waitInstanceId:[{}], status=[{}] skipping ...", waitInstanceId,
          waitInstance.getStatus());
      return;
    }

    PageRequest<WaitQueue> req = new PageRequest<>();
    req.addFilter("waitInstanceId", waitInstanceId, SearchFilter.Operator.EQ);
    PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req, ReadPref.CRITICAL);

    if (isEmpty(waitQueuesResponse)) {
      logger.warn("No entry in the waitQueue found for the waitInstanceId:[{}] skipping ...", waitInstanceId);
      return;
    }

    List<String> correlationIds = message.getCorrelationIds();
    final List<String> finalCorrelationIdsForLambda = correlationIds;

    if (!isEmpty(correlationIds)) {
      List<String> missingCorrelationIds = waitQueuesResponse.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !finalCorrelationIdsForLambda.contains(s))
                                               .collect(toList());
      if (!isEmpty(missingCorrelationIds)) {
        logger.warn("Some of the correlationIds still needs to be waited, waitInstanceId: [{}], correlationIds: {}",
            waitInstanceId, missingCorrelationIds);
        return;
      }
    }

    Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();
    Map<String, NotifyResponseData> responseMap = new HashMap<>();

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.setFieldName(ID_KEY);
    searchFilter.setFieldValues(
        waitQueuesResponse.stream().map(WaitQueue::getCorrelationId).collect(toList()).toArray());
    searchFilter.setOp(SearchFilter.Operator.IN);
    PageRequest<NotifyResponse> notifyResponseReq = new PageRequest<>();
    notifyResponseReq.addFilter(searchFilter);
    PageResponse<NotifyResponse> notifyResponses =
        wingsPersistence.query(NotifyResponse.class, notifyResponseReq, ReadPref.CRITICAL);

    correlationIds = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toList());

    final List<String> finalCorrelationIds = correlationIds;
    if (notifyResponses.size() != waitQueuesResponse.size()) {
      List<String> missingCorrelationIds = waitQueuesResponse.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !finalCorrelationIds.contains(s))
                                               .collect(toList());
      logger.warn(
          "notifyResponses for the correlationIds: {} not found. skipping the callback for the waitInstanceId: [{}]",
          missingCorrelationIds, waitInstanceId);
      return;
    }

    notifyResponses.forEach(notifyResponse -> {
      responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
      notifyResponseMap.put(notifyResponse.getUuid(), notifyResponse);
    });

    boolean isError = notifyResponses.stream().filter(NotifyResponse::isError).findFirst().isPresent();

    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(WaitInstance.class, waitInstanceId);
      if (!lockAcquired) {
        logger.warn("Persistent lock could not be acquired for the waitInstanceId: " + waitInstanceId);
        return;
      }

      ExecutionStatus status = ExecutionStatus.SUCCESS;
      NotifyCallback callback = waitInstance.getCallback();
      injector.injectMembers(callback);
      if (callback != null) {
        try {
          if (isError) {
            callback.notifyError(responseMap);
          } else {
            callback.notify(responseMap);
          }
        } catch (Exception exception) {
          status = ExecutionStatus.ERROR;
          logger.error("WaitInstance callback failed - waitInstanceId:" + waitInstanceId, exception);
          try {
            WaitInstanceError waitInstanceError = new WaitInstanceError();
            waitInstanceError.setWaitInstanceId(waitInstanceId);
            waitInstanceError.setResponseMap(responseMap);
            waitInstanceError.setErrorStackTrace(ExceptionUtils.getStackTrace(exception));

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
      } catch (Exception exception) {
        logger.error("Error in waitInstanceUpdate", exception);
      }

      UpdateOperations<NotifyResponse> notifyResponseUpdate =
          wingsPersistence.createUpdateOperations(NotifyResponse.class).set("status", ExecutionStatus.SUCCESS);
      for (WaitQueue waitQueue : waitQueuesResponse) {
        try {
          wingsPersistence.delete(waitQueue);
          wingsPersistence.update(notifyResponseMap.get(waitQueue.getCorrelationId()), notifyResponseUpdate);
        } catch (Exception exception) {
          logger.error("Error in waitQueue cleanup", exception);
        }
      }
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(WaitInstance.class, waitInstanceId);
      }
    }
    logger.trace("Done processing message {}", message);
  }
}
