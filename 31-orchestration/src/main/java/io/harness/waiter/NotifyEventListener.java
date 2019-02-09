package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.WriteConcern;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.queue.QueueListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public final class NotifyEventListener extends QueueListener<NotifyEvent> {
  private static final Logger logger = LoggerFactory.getLogger(NotifyEventListener.class);

  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  @Inject private Injector injector;
  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;

  public NotifyEventListener() {
    super(false);
  }

  FindAndModifyOptions OPTIONS =
      new FindAndModifyOptions().writeConcern(WriteConcern.MAJORITY).upsert(false).returnNew(false);

  private WaitInstance getWaitInstance(String waitInstanceId, long now) {
    final long limit = now - MAX_CALLBACK_PROCESSING_TIME.toMillis();

    final Query<WaitInstance> waitInstanceQuery = persistence.createQuery(WaitInstance.class, ReadPref.CRITICAL)
                                                      .filter(WaitInstance.ID_KEY, waitInstanceId)
                                                      .filter(WaitInstance.STATUS_KEY, ExecutionStatus.NEW)
                                                      .field(WaitInstance.CALLBACK_PROCESSING_AT_KEY)
                                                      .lessThan(limit);

    final UpdateOperations<WaitInstance> updateOperations =
        persistence.createUpdateOperations(WaitInstance.class).set(WaitInstance.CALLBACK_PROCESSING_AT_KEY, now);

    return persistence.getDatastore(WaitInstance.class, ReadPref.CRITICAL)
        .findAndModify(waitInstanceQuery, updateOperations, OPTIONS);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing message {}", message);
    }

    String waitInstanceId = message.getWaitInstanceId();

    List<WaitQueue> waitQueues = persistence.createQuery(WaitQueue.class, ReadPref.CRITICAL, excludeAuthority)
                                     .filter(WaitQueue.WAIT_INSTANCE_ID_KEY, waitInstanceId)
                                     .asList();

    if (isEmpty(waitQueues)) {
      logger.warn("No entry in the waitQueue found for the waitInstanceId:[{}] skipping ...", waitInstanceId);
      return;
    }

    List<String> correlationIds = message.getCorrelationIds();
    if (isNotEmpty(correlationIds)) {
      Set<String> correlationIdSet = new HashSet<>(correlationIds);
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !correlationIdSet.contains(s))
                                               .collect(toList());
      if (isNotEmpty(missingCorrelationIds)) {
        logger.info("Some of the correlationIds still needs to be waited, waitInstanceId: [{}], correlationIds: {}",
            waitInstanceId, missingCorrelationIds);
        return;
      }
    }

    final List<NotifyResponse> notifyResponses =
        persistence.createQuery(NotifyResponse.class, ReadPref.CRITICAL, excludeAuthority)
            .field(ID_KEY)
            .in(waitQueues.stream().map(WaitQueue::getCorrelationId).collect(toList()))
            .asList();

    Set<String> correlationIdSet = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toSet());
    if (notifyResponses.size() != waitQueues.size()) {
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !correlationIdSet.contains(s))
                                               .collect(toList());
      logger.warn(
          "notifyResponses for the correlationIds: {} not found. skipping the callback for the waitInstanceId: [{}]",
          missingCorrelationIds, waitInstanceId);
      return;
    }

    final long now = System.currentTimeMillis();
    WaitInstance waitInstance = getWaitInstance(waitInstanceId, now);
    if (waitInstance == null) {
      // This instance is already handled.
      return;
    }

    Map<String, ResponseData> responseMap = new HashMap<>();
    Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();

    notifyResponses.forEach(notifyResponse -> {
      responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
      notifyResponseMap.put(notifyResponse.getUuid(), notifyResponse);
    });

    boolean isError = notifyResponses.stream().filter(NotifyResponse::isError).findFirst().isPresent();

    ExecutionStatus status = ExecutionStatus.SUCCESS;
    NotifyCallback callback = waitInstance.getCallback();
    if (callback != null) {
      injector.injectMembers(callback);
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
          WaitInstanceError waitInstanceError = WaitInstanceError.builder()
                                                    .waitInstanceId(waitInstanceId)
                                                    .responseMap(responseMap)
                                                    .errorStackTrace(ExceptionUtils.getStackTrace(exception))
                                                    .build();

          persistence.save(waitInstanceError);
        } catch (Exception e2) {
          logger.error("Error in persisting waitInstanceError", e2);
        }
      }
    }

    // time to cleanup
    try {
      UpdateOperations<WaitInstance> waitInstanceUpdate =
          persistence.createUpdateOperations(WaitInstance.class)
              .set(WaitInstance.STATUS_KEY, status)
              .set(WaitInstance.VALID_UNTIL_KEY,
                  Date.from(OffsetDateTime.now().plus(WaitInstance.AfterFinishTTL).toInstant()));
      persistence.update(waitInstance, waitInstanceUpdate);
    } catch (Exception exception) {
      logger.error("Error in waitInstanceUpdate", exception);
    }

    final long passed = System.currentTimeMillis() - now;
    if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
      logger.error("It took more than {} ms before we processed the notification. THIS IS VERY BAD!!!",
          MAX_CALLBACK_PROCESSING_TIME.toMillis());
    }

    UpdateOperations<NotifyResponse> notifyResponseUpdate =
        persistence.createUpdateOperations(NotifyResponse.class).set("status", ExecutionStatus.SUCCESS);
    for (WaitQueue waitQueue : waitQueues) {
      try {
        persistence.delete(waitQueue);
        persistence.update(notifyResponseMap.get(waitQueue.getCorrelationId()), notifyResponseUpdate);
      } catch (Exception exception) {
        logger.error("Error in waitQueue cleanup", exception);
      }
    }

    logger.trace("Done processing message {}", message);
  }
}
