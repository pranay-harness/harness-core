package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.utils.Misc;

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

    log().debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));

    String waitInstanceId = wingsPersistence.save(new WaitInstance(callback, correlationIds));

    // create queue
    Arrays.stream(correlationIds)
        .forEach(correlationId -> wingsPersistence.save(new WaitQueue(waitInstanceId, correlationId)));

    return waitInstanceId;
  }

  private Logger log() {
    return LoggerFactory.getLogger(WaitNotifyEngine.class);
  }

  /**
   * Notifies WaitNotifyEngine when a correlationId is finished.
   *
   * @param <T>           ResponseObject type should be serializable.
   * @param correlationId id which is finished.
   * @param response      response object for the task.
   * @return id of notification response object.
   */
  public <T extends NotifyResponseData> String notify(String correlationId, T response) {
    return notify(correlationId, response, false);
  }

  public String notify(String correlationId, ErrorNotifyResponseData response) {
    return notify(correlationId, response, true);
  }

  private <T extends NotifyResponseData> String notify(String correlationId, T response, boolean error) {
    Preconditions.checkArgument(isNotEmpty(correlationId), "correlationId is null or empty");

    log().debug("notify request received for the correlationId : {}", correlationId);

    try {
      String notificationId = wingsPersistence.save(new NotifyResponse(correlationId, response, error));

      PageRequest<WaitQueue> req = new PageRequest<>();
      req.addFilter("correlationId", correlationId, SearchFilter.Operator.EQ);
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req, ReadPref.CRITICAL);
      waitQueuesResponse.forEach(waitQueue
          -> notifyQueue.send(
              aNotifyEvent().withWaitInstanceId(waitQueue.getWaitInstanceId()).withError(error).build()));

      return notificationId;
    } catch (Exception e) {
      logger.error(
          "Failed to notify for response of type " + response.getClass().getSimpleName() + ": " + Misc.getMessage(e),
          e);
    }
    return null;
  }
}
