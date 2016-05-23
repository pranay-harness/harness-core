package software.wings.waitnotify;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;

import java.io.Serializable;
import java.util.Arrays;

import javax.inject.Inject;

/**
 * WaitNotifyEngine allows tasks to register in waitQueue and get notified via callback.
 *
 * @author Rishi
 */
@Singleton
public class WaitNotifyEngine {
  private static final long NO_TIMEOUT = 0L;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private Queue<NotifyEvent> notifyQueue;

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
   * @param correlationId id which is finished.
   * @param response      response object for the task.
   * @param <T>           ResponseObject type should be serializable.
   * @return id of notification response object.
   */
  public <T extends Serializable> String notify(String correlationId, T response) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(correlationId), "correlationId is null or empty");

    log().debug("notify request received for the correlationId : {}", correlationId);

    String notificationId = wingsPersistence.save(new NotifyResponse<T>(correlationId, response));

    PageRequest<WaitQueue> req = new PageRequest<>();
    req.addFilter("correlationId", correlationId, SearchFilter.Operator.EQ);
    PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req, ReadPref.CRITICAL);
    waitQueuesResponse.forEach(
        waitQueue -> notifyQueue.send(aNotifyEvent().withWaitInstanceId(waitQueue.getWaitInstanceId()).build()));

    return notificationId;
  }
}
