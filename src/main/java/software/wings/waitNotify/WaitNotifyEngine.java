package software.wings.waitNotify;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import software.wings.common.thread.ThreadPool;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;

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

  @Inject private PersistentLocker persistentLocker;

  @Inject private ExecutorService executorService;

  public String waitForAll(NotifyCallback callback, String... correlationIds) {
    return waitForAll(NO_TIMEOUT, callback, correlationIds);
  }

  public String waitForAll(long timeoutMsec, NotifyCallback callback, String... correlationIds) {
    if (correlationIds == null || correlationIds.length == 0) {
      throw new IllegalArgumentException("correlationIds are null or empty");
    }

    logger.debug("Received waitForAll on - correlationIds : " + Arrays.toString(correlationIds));

    WaitInstance waitInstance = new WaitInstance(callback, correlationIds);
    String waitInstanceId = wingsPersistence.save(waitInstance);

    // create queue
    for (String correlationId : correlationIds) {
      WaitQueue queue = new WaitQueue(waitInstanceId, correlationId);
      wingsPersistence.save(queue);
    }

    return waitInstanceId;
  }

  public String notify(String correlationId, Serializable response) {
    if (StringUtils.isEmpty(correlationId)) {
      throw new IllegalArgumentException("correlationIds are null or empty");
    }
    logger.debug("notify request received for the correlationId :" + correlationId);
    String notificationId = wingsPersistence.save(new NotifyResponse(correlationId, response));
    executorService.submit(new Notifier(wingsPersistence, persistentLocker, executorService));
    return notificationId;
  }

  private static Logger logger = LoggerFactory.getLogger(WaitNotifyEngine.class);
}
