package software.wings.waitnotify;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jetty.util.LazyList.isEmpty;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;
import software.wings.sm.ExecutionStatus;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Singleton
public class NotifyEventListener extends AbstractQueueListener<NotifyEvent> {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  @Override
  protected void onMessage(NotifyEvent message) throws Exception {
    log().trace("Processing message {}", message);
    String waitInstanceId = message.getWaitInstanceId();

    WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId);

    if (waitInstance == null) {
      log().warn("waitInstance not found for waitInstanceId: {}", waitInstanceId);
      return;
    }
    if (waitInstance.getStatus() != ExecutionStatus.NEW) {
      log().warn("WaitInstance already processed - waitInstanceId:[{}], status=[{}] skipping ...", waitInstanceId,
          waitInstance.getStatus());
      return;
    }

    PageRequest<WaitQueue> req = new PageRequest<>();
    req.addFilter("waitInstanceId", waitInstanceId, SearchFilter.OP.EQ);
    PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);

    if (waitQueuesResponse == null || isEmpty(waitQueuesResponse.getResponse())) {
      log().warn("No entry in the waitQueue found for the waitInstanceId:[{}] skipping ...", waitInstanceId);
      return;
    }

    List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
    List<String> correlationIds = message.getCorrelationIds();
    final List<String> finalCorrelationIdsForLambda = correlationIds;

    if (!isEmpty(correlationIds)) {
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !finalCorrelationIdsForLambda.contains(s))
                                               .collect(toList());
      if (!isEmpty(missingCorrelationIds)) {
        log().warn("Some of the correlationIds still needs to be waited, "
                + "waitInstanceId: [{}], correlationIds: {}",
            waitInstanceId, missingCorrelationIds);
        return;
      }
    }

    Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();
    Map<String, Serializable> responseMap = new HashMap<>();

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.setFieldName("uuid");
    searchFilter.setFieldValues(waitQueues.stream().map(WaitQueue::getCorrelationId).collect(toList()));
    searchFilter.setOp(SearchFilter.OP.IN);
    PageRequest<NotifyResponse> notifyResponseReq = new PageRequest<>();
    notifyResponseReq.setFilters(Collections.singletonList(searchFilter));
    PageResponse<NotifyResponse> notifyResponses = wingsPersistence.query(NotifyResponse.class, notifyResponseReq);

    correlationIds = notifyResponses.getResponse().stream().map(NotifyResponse::getUuid).collect(toList());

    final List<String> finalCorrelationIds = correlationIds;
    if (notifyResponses.getResponse().size() != waitQueues.size()) {
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !finalCorrelationIds.contains(s))
                                               .collect(toList());
      log().error("notifyResponses for the correlationIds: {} not found."
              + " skipping the callback for the waitInstanceId: [{}]",
          missingCorrelationIds, waitInstanceId);
      return;
    }

    notifyResponses.getResponse().forEach(notifyResponse -> {
      responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
      notifyResponseMap.put(notifyResponse.getUuid(), notifyResponse);
    });

    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(WaitInstance.class, waitInstanceId);
      if (!lockAcquired) {
        log().warn("Persistent lock could not be acquired for the waitInstanceId: " + waitInstanceId);
        return;
      }

      ExecutionStatus status = ExecutionStatus.SUCCESS;
      NotifyCallback callback = waitInstance.getCallback();
      if (callback != null) {
        try {
          callback.notify(responseMap);
        } catch (Exception exception) {
          status = ExecutionStatus.ERROR;
          log().error("WaitInstance callback failed - waitInstanceId:" + waitInstanceId, exception);
          try {
            WaitInstanceError waitInstanceError = new WaitInstanceError();
            waitInstanceError.setWaitInstanceId(waitInstanceId);
            waitInstanceError.setResponseMap(responseMap);
            waitInstanceError.setErrorStackTrace(ExceptionUtils.getStackTrace(exception));

            wingsPersistence.save(waitInstanceError);
          } catch (Exception e2) {
            log().error("Error in persisting waitInstanceError", e2);
          }
        }
      }

      // time to cleanup
      try {
        UpdateOperations<WaitInstance> waitInstanceUpdate =
            wingsPersistence.createUpdateOperations(WaitInstance.class).set("status", status);
        wingsPersistence.update(waitInstance, waitInstanceUpdate);
      } catch (Exception exception) {
        log().error("Error in waitInstanceUpdate", exception);
      }

      UpdateOperations<NotifyResponse> notifyResponseUpdate =
          wingsPersistence.createUpdateOperations(NotifyResponse.class).set("status", ExecutionStatus.SUCCESS);
      for (WaitQueue waitQueue : waitQueues) {
        try {
          wingsPersistence.delete(waitQueue);
          wingsPersistence.update(notifyResponseMap.get(waitQueue.getCorrelationId()), notifyResponseUpdate);
        } catch (Exception exception) {
          log().error("Error in waitQueue cleanup", exception);
        }
      }
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(WaitInstance.class, waitInstanceId);
      }
    }
    log().trace("Done processing message {}", message);
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
