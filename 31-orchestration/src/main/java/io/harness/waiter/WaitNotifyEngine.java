package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ResponseData;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.queue.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * WaitNotifyEngine allows tasks to register in waitQueue and get notified via callback.
 * No entry in the waitQueue found for the correlationIds:
 */
@Singleton
public class WaitNotifyEngine {
  private static final Logger logger = LoggerFactory.getLogger(WaitNotifyEngine.class);

  private static final long NO_TIMEOUT = 0L;

  @Inject private HPersistence wingsPersistence;

  @Inject private Queue<NotifyEvent> notifyQueue;

  /**
   * Wait for all.
   *
   * @param callback       the callback
   * @param correlationIds the correlation ids
   * @return the string
   */
  public String waitForAll(NotifyCallback callback, String... correlationIds) {
    return waitForAll(NO_TIMEOUT, callback, correlationIds);
  }

  /**
   * Allows a task to register a callback to wait for when given correlationIds and done.
   *
   * @param timeoutMsec    timeout for wait in milliseconds.
   * @param callback       function to be executed when all correlationIds are completed.
   * @param correlationIds list of ids to wait for.
   * @return id of WaitInstance.
   */
  public String waitForAll(long timeoutMsec, NotifyCallback callback, String... correlationIds) {
    Preconditions.checkArgument(isNotEmpty(correlationIds), "correlationIds are null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));
    }

    // It is important we to save the wait instance first and then the wait queues. From now on we will
    // assume that wait queue without an instance is a zombie and we are going to remove it.
    String waitInstanceId = wingsPersistence.save(
        WaitInstance.builder().uuid(generateUuid()).callback(callback).correlationIds(asList(correlationIds)).build());

    // create queue
    for (String correlationId : correlationIds) {
      wingsPersistence.save(WaitQueue.builder()
                                .uuid(generateUuid())
                                .createdAt(currentTimeMillis())
                                .waitInstanceId(waitInstanceId)
                                .correlationId(correlationId)
                                .build());
    }

    return waitInstanceId;
  }

  /**
   * Notifies WaitNotifyEngine when a correlationId is finished.
   *
   * @param <T>           ResponseObject type should be serializable.
   * @param correlationId id which is finished.
   * @param response      response object for the task.
   * @return id of notification response object.
   */
  public <T extends ResponseData> String notify(String correlationId, T response) {
    return notify(correlationId, response, response instanceof ErrorNotifyResponseData);
  }

  // If we get duplicate key exception this means that this correlation id was already handled. In rare cases
  // the delegate might get confused and send the response twice. Ignore few DuplicateKeyException per hour.
  // If more are observed that's alarming.
  static RateLimiter duplicateKeyExceptionRateLimiter = RateLimiter.create(8.0 / Duration.ofHours(1).getSeconds());

  private <T extends ResponseData> String notify(String correlationId, T response, boolean error) {
    Preconditions.checkArgument(isNotBlank(correlationId), "correlationId is null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("notify request received for the correlationId : {}", correlationId);
    }

    try {
      String notificationId = wingsPersistence.save(NotifyResponse.<T>builder()
                                                        .uuid(correlationId)
                                                        .createdAt(currentTimeMillis())
                                                        .response(response)
                                                        .error(error)
                                                        .build());

      final List<WaitQueue> waitQueues =
          wingsPersistence.createQuery(WaitQueue.class, ReadPref.CRITICAL, excludeAuthority)
              .filter(WaitQueue.CORRELATION_ID_KEY, correlationId)
              .asList();

      waitQueues.forEach(waitQueue
          -> notifyQueue.send(aNotifyEvent().waitInstanceId(waitQueue.getWaitInstanceId()).error(error).build()));

      return notificationId;
    } catch (DuplicateKeyException exception) {
      if (!duplicateKeyExceptionRateLimiter.tryAcquire()) {
        logger.error("Unexpected rate of DuplicateKeyException per correlation", exception);
      }
    } catch (Exception exception) {
      logger.error("Failed to notify for response of type " + response.getClass().getSimpleName(), exception);
    }
    return null;
  }
}
