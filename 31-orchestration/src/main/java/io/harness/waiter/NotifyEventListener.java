package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.WriteConcern;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListener;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public final class NotifyEventListener extends QueueListener<NotifyEvent> {
  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  @Inject private Injector injector;
  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;

  public NotifyEventListener() {
    super(false);
  }

  FindAndModifyOptions findAndModifyOptions =
      new FindAndModifyOptions().writeConcern(WriteConcern.MAJORITY).upsert(false).returnNew(false);

  private WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now) {
    final long limit = now - MAX_CALLBACK_PROCESSING_TIME.toMillis();

    final Query<WaitInstance> waitInstanceQuery = persistence.createQuery(WaitInstance.class)
                                                      .filter(WaitInstanceKeys.uuid, waitInstanceId)
                                                      .filter(WaitInstanceKeys.status, ExecutionStatus.NEW)
                                                      .field(WaitInstanceKeys.callbackProcessingAt)
                                                      .lessThan(limit);

    final UpdateOperations<WaitInstance> updateOperations =
        persistence.createUpdateOperations(WaitInstance.class).set(WaitInstanceKeys.callbackProcessingAt, now);

    return persistence.findAndModify(waitInstanceQuery, updateOperations, findAndModifyOptions);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing message {}", message);
    }

    String waitInstanceId = message.getWaitInstanceId();

    WaitInstance waitInstance = persistence.createQuery(WaitInstance.class, excludeAuthority)
                                    .filter(WaitInstanceKeys.uuid, waitInstanceId)
                                    .get();

    if (waitInstance == null) {
      logger.warn("No waitInstance with id:[{}] skipping ...", waitInstanceId);
      return;
    }

    if (isNotEmpty(message.getCorrelationIds())) {
      if (message.getCorrelationIds().size() < waitInstance.getCorrelationIds().size()) {
        logger.info("Some of the correlationIds still needs to be waited, waitInstanceId: [{}]", waitInstanceId);
        return;
      }
    }

    final List<NotifyResponse> notifyResponses = persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                                     .field(ID_KEY)
                                                     .in(waitInstance.getCorrelationIds())
                                                     .asList();

    if (notifyResponses.size() != waitInstance.getCorrelationIds().size()) {
      logger.warn(
          "some notifyResponses for not found. Skipping the callback for the waitInstanceId: [{}]", waitInstanceId);
      return;
    }

    final long now = System.currentTimeMillis();
    waitInstance = fetchForProcessingWaitInstance(waitInstanceId, now);
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

    boolean isError = notifyResponses.stream().anyMatch(NotifyResponse::isError);

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
              .set(WaitInstanceKeys.status, status)
              .set(WaitInstanceKeys.validUntil,
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
    for (String correlationId : waitInstance.getCorrelationIds()) {
      try {
        persistence.update(notifyResponseMap.get(correlationId), notifyResponseUpdate);
      } catch (Exception exception) {
        logger.error("Error in waitQueue cleanup", exception);
      }
    }

    logger.trace("Done processing message {}", message);
  }
}
