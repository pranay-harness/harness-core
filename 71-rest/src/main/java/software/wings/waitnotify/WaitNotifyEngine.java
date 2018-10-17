package software.wings.waitnotify;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.persistence.ReadPref;
import io.harness.queue.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.Arrays;

/**
 * WaitNotifyEngine allows tasks to register in waitQueue and get notified via callback.
 * No entry in the waitQueue found for the correlationIds:
 *
 * @author Rishi
 */
@Singleton
public class WaitNotifyEngine {
  private static final Logger logger = LoggerFactory.getLogger(WaitNotifyEngine.class);

  private static final long NO_TIMEOUT = 0L;

  @Inject private WingsPersistence wingsPersistence;

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

    String waitInstanceId = wingsPersistence.save(new WaitInstance(callback, correlationIds));

    // create queue
    for (String correlationId : correlationIds) {
      wingsPersistence.save(new WaitQueue(waitInstanceId, correlationId));
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
      String notificationId = wingsPersistence.save(new NotifyResponse(correlationId, response, error));

      PageRequest<WaitQueue> req =
          aPageRequest().withReadPref(ReadPref.CRITICAL).addFilter("correlationId", EQ, correlationId).build();
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req, excludeAuthority);
      waitQueuesResponse.forEach(waitQueue
          -> notifyQueue.send(
              aNotifyEvent().withWaitInstanceId(waitQueue.getWaitInstanceId()).withError(error).build()));

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
