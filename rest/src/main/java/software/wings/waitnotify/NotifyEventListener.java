package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.persistence.ReadPref;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.sm.ExecutionStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Singleton
public final class NotifyEventListener extends AbstractQueueListener<NotifyEvent> {
  private static final Logger logger = LoggerFactory.getLogger(NotifyEventListener.class);

  @Inject private Injector injector;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  public NotifyEventListener() {
    super(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onMessage(NotifyEvent message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing message {}", message);
    }

    String waitInstanceId = message.getWaitInstanceId();

    WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId, ReadPref.CRITICAL);

    if (waitInstance == null) {
      logger.info("waitInstance not found for waitInstanceId: {}", waitInstanceId);
      return;
    }
    if (waitInstance.getStatus() != ExecutionStatus.NEW) {
      logger.warn("WaitInstance already processed - waitInstanceId:[{}], status=[{}] skipping ...", waitInstanceId,
          waitInstance.getStatus());
      return;
    }

    PageRequest<WaitQueue> req = aPageRequest()
                                     .withLimit(UNLIMITED)
                                     .withReadPref(ReadPref.CRITICAL)
                                     .addFilter("waitInstanceId", EQ, waitInstanceId)
                                     .build();
    PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req, excludeAuthority);

    if (isEmpty(waitQueuesResponse)) {
      logger.warn("No entry in the waitQueue found for the waitInstanceId:[{}] skipping ...", waitInstanceId);
      return;
    }

    List<String> correlationIds = message.getCorrelationIds();
    final List<String> finalCorrelationIdsForLambda = correlationIds;

    if (isNotEmpty(correlationIds)) {
      if (correlationIds.size() * waitQueuesResponse.size() > 100) {
        logger.error("Correlation/WaitQueue O(N*M) algorithm needs to be optimized");
      }

      List<String> missingCorrelationIds = waitQueuesResponse.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !finalCorrelationIdsForLambda.contains(s))
                                               .collect(toList());
      if (isNotEmpty(missingCorrelationIds)) {
        logger.info("Some of the correlationIds still needs to be waited, waitInstanceId: [{}], correlationIds: {}",
            waitInstanceId, missingCorrelationIds);
        return;
      }
    }

    PageRequest<NotifyResponse> notifyResponseReq =
        aPageRequest()
            .withReadPref(ReadPref.CRITICAL)
            .addFilter(ID_KEY, Operator.IN,
                waitQueuesResponse.stream().map(WaitQueue::getCorrelationId).collect(toList()).toArray())
            .withLimit(PageRequest.UNLIMITED)
            .build();
    PageResponse<NotifyResponse> notifyResponses =
        wingsPersistence.query(NotifyResponse.class, notifyResponseReq, excludeAuthority);

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

    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();

    notifyResponses.forEach(notifyResponse -> {
      responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
      notifyResponseMap.put(notifyResponse.getUuid(), notifyResponse);
    });

    boolean isError = notifyResponses.stream().filter(NotifyResponse::isError).findFirst().isPresent();

    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(WaitInstance.class, waitInstanceId, Duration.ofMinutes(1))) {
      if (lock == null) {
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
    }
    logger.trace("Done processing message {}", message);
  }
}
