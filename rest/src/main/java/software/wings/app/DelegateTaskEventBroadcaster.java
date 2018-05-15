package software.wings.app;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.CREATED_AT_KEY;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import org.atmosphere.cpr.BroadcasterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by anubhaw on 5/14/18.
 */
public class DelegateTaskEventBroadcaster implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DelegateQueueTask.class);
  public static final String STATUS_KEY = "status";
  public static final String ASYNC_KEY = "async";
  public static final String DELEGATE_ID_KEY = "delegateId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private BroadcasterFactory broadcasterFactory;

  private static AtomicLong lastQueriedAt = new AtomicLong(0);

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance()) {
      return;
    }
    try {
      wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
          .filter(STATUS_KEY, Status.QUEUED)
          .filter(ASYNC_KEY, false)
          .field(DELEGATE_ID_KEY)
          .doesNotExist()
          .field(CREATED_AT_KEY)
          .greaterThan(lastQueriedAt.get())
          .project(ACCOUNT_ID_KEY, true)
          .project(CREATED_AT_KEY, true)
          .fetch()
          .forEach(this ::broadcast);

    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the DelegateTaskEventBroadcaster runnable", exception);
    }
  }

  protected void broadcast(DelegateTask delegateTask) {
    if (lastQueriedAt.get() < delegateTask.getCreatedAt()) {
      lastQueriedAt.set(delegateTask.getCreatedAt());
    }
    logger.info("Broadcast queued task [{}]", delegateTask.getUuid());
    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
  }
}
