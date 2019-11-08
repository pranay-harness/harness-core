package io.harness.waiter;

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
    final Query<WaitInstance> waitInstanceQuery = persistence.createQuery(WaitInstance.class)
                                                      .filter(WaitInstanceKeys.uuid, waitInstanceId)
                                                      .filter(WaitInstanceKeys.status, ExecutionStatus.NEW)
                                                      .field(WaitInstanceKeys.callbackProcessingAt)
                                                      .lessThan(now);

    final UpdateOperations<WaitInstance> updateOperations =
        persistence.createUpdateOperations(WaitInstance.class)
            .set(WaitInstanceKeys.callbackProcessingAt, now + MAX_CALLBACK_PROCESSING_TIME.toMillis());

    return persistence.findAndModify(waitInstanceQuery, updateOperations, findAndModifyOptions);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing message {}", message);
    }

    String waitInstanceId = message.getWaitInstanceId();

    final long now = System.currentTimeMillis();
    WaitInstance waitInstance = fetchForProcessingWaitInstance(waitInstanceId, now);

    if (waitInstance == null) {
      logger.error("Double notification for the same waitInstance: {}", waitInstanceId);
      return;
    }

    final List<NotifyResponse> notifyResponses = persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                                     .field(ID_KEY)
                                                     .in(waitInstance.getCorrelationIds())
                                                     .asList();

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
  }
}
