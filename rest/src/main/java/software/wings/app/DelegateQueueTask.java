package software.wings.app;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.atmosphere.cpr.BroadcasterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.PersistentLocker;
import software.wings.utils.CacheHelper;

import java.util.Spliterator;
import javax.inject.Inject;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class DelegateQueueTask implements Runnable {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  @Inject private BroadcasterFactory broadcasterFactory;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(DelegateQueueTask.class, DelegateQueueTask.class.getName());
      if (!lockAcquired) {
        log().warn("Persistent lock could not be acquired for the DelegateQueue");
        return;
      }

      stream(
          spliteratorUnknownSize(CacheHelper.getCache("delegateSyncCache", String.class, DelegateTask.class).iterator(),
              Spliterator.NONNULL),
          false)
          .filter(stringDelegateTaskEntry -> stringDelegateTaskEntry.getValue().getStatus().equals(Status.QUEUED))
          .forEach(stringDelegateTaskEntry
              -> broadcasterFactory
                     .lookup("/stream/delegate/" + stringDelegateTaskEntry.getValue().getAccountId(), true)
                     .broadcast(stringDelegateTaskEntry.getValue()));

      PageResponse<DelegateTask> delegateTasks = wingsPersistence.query(
          DelegateTask.class, aPageRequest().addFilter("status", Operator.EQ, Status.QUEUED).build());

      if (isEmpty(delegateTasks)) {
        log().debug("There are no delegateTasks to process");
        return;
      }

      delegateTasks.getResponse().forEach(delegateTask
          -> broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true)
                 .broadcast(delegateTask));

    } catch (Exception exception) {
      log().error("Error seen in the Notifier call", exception);
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(DelegateQueueTask.class, DelegateQueueTask.class.getName());
      }
    }
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
