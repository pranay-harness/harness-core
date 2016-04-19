package software.wings.waitNotify;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static software.wings.waitNotify.NotifyEvent.Builder.aNotifyEvent;

/**
 *  WaitNotifyEngine
 *
 *
 * @author Rishi
 *
 */

@Singleton
public class WaitNotifyEngine {
  private static final long NO_TIMEOUT = 0l;
  private static WaitNotifyEngine instance;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private Queue<NotifyEvent> notifyQueue;

  public String waitForAll(NotifyCallback callback, String... correlationIds) {
    return waitForAll(NO_TIMEOUT, callback, correlationIds);
  }

  public String waitForAll(long timeoutMsec, NotifyCallback callback, String... correlationIds) {
    Preconditions.checkArgument(isNotEmpty(correlationIds), "correlationIds are null or empty");

    log().debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));

    String waitInstanceId = wingsPersistence.save(new WaitInstance(callback, correlationIds));

    // create queue
    Arrays.stream(correlationIds)
        .forEach(correlationId -> wingsPersistence.save(new WaitQueue(waitInstanceId, correlationId)));

    return waitInstanceId;
  }

  public <T extends Serializable> String notify(String correlationId, T response) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(correlationId), "correlationId is null or empty");

    log().debug("notify request received for the correlationId : {}", correlationId);

    String notificationId = wingsPersistence.save(new NotifyResponse(correlationId, response));

    PageRequest<WaitQueue> req = new PageRequest<>();
    req.addFilter("correlationId", correlationId, SearchFilter.OP.EQ);
    PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
    waitQueuesResponse.getResponse().forEach(
        waitQueue -> notifyQueue.send(aNotifyEvent().withWaitInstanceId(waitQueue.getWaitInstanceId()).build()));

    return notificationId;
  }

  private Logger log() {
    return LoggerFactory.getLogger(WaitNotifyEngine.class);
  }
}
