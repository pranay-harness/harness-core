package io.harness.waiter;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.mongodb.WriteConcern;
import io.harness.delegate.beans.ResponseData;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class NotifyEventListener extends QueueListener<NotifyEvent> {
  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  @Inject private Injector injector;
  @Inject private HPersistence persistence;

  @Inject
  public NotifyEventListener(QueueConsumer<NotifyEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  FindAndModifyOptions findAndModifyOptions =
      new FindAndModifyOptions().writeConcern(WriteConcern.MAJORITY).upsert(false).returnNew(false);

  private WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now) {
    final Query<WaitInstance> waitInstanceQuery = persistence.createQuery(WaitInstance.class)
                                                      .filter(WaitInstanceKeys.uuid, waitInstanceId)
                                                      .field(WaitInstanceKeys.callbackProcessingAt)
                                                      .lessThan(now);

    final UpdateOperations<WaitInstance> updateOperations =
        persistence.createUpdateOperations(WaitInstance.class)
            .set(WaitInstanceKeys.callbackProcessingAt, now + MAX_CALLBACK_PROCESSING_TIME.toMillis());

    return persistence.findAndModify(waitInstanceQuery, updateOperations, findAndModifyOptions);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    String waitInstanceId = message.getWaitInstanceId();

    try (AutoLogContext ignore = new WaitInstanceLogContext(message.getWaitInstanceId(), OVERRIDE_ERROR)) {
      final long now = System.currentTimeMillis();
      WaitInstance waitInstance = fetchForProcessingWaitInstance(waitInstanceId, now);

      if (waitInstance == null) {
        logger.error("Double notification");
        return;
      }

      boolean isError = false;
      Map<String, ResponseData> responseMap = new HashMap<>();

      try (HIterator<NotifyResponse> notifyResponses =
               new HIterator(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                 .field(NotifyResponseKeys.uuid)
                                 .in(waitInstance.getCorrelationIds())
                                 .fetch())) {
        for (NotifyResponse notifyResponse : notifyResponses) {
          if (notifyResponse.isError()) {
            logger.info("Failed notification response {}", notifyResponse.getUuid());
            isError = true;
          }
          responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
        }
      }

      NotifyCallback callback = waitInstance.getCallback();
      if (callback != null) {
        injector.injectMembers(callback);
        try {
          if (isError) {
            callback.notifyError(responseMap);
          } else {
            callback.notify(responseMap);
          }
          logger.info("WaitInstance callback finished");
        } catch (Exception exception) {
          logger.error("WaitInstance callback failed", exception);
        }
      }

      try {
        persistence.delete(waitInstance);
      } catch (Exception exception) {
        logger.error("Failed to delete WaitInstance", exception);
      }

      final long passed = System.currentTimeMillis() - now;
      if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
        logger.error("It took more than {} ms before we processed the callback. THIS IS VERY BAD!!!",
            MAX_CALLBACK_PROCESSING_TIME.toMillis());
      }
    }
  }
}
