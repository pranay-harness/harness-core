package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.time.Duration.ofMinutes;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTask.Status.STARTED;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.service.impl.DelegateServiceImpl.VALIDATION_TIMEOUT;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.version.VersionInfoManager;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.WhereCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class DelegateQueueTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DelegateQueueTask.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private TimeLimiter timeLimiter;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance()) {
      return;
    }

    try (AcquiredLock ignore =
             persistentLocker.acquireLock(DelegateQueueTask.class, DelegateQueueTask.class.getName(), ofMinutes(1))) {
      timeLimiter.callWithTimeout(() -> {
        markTimedOutTasksAsFailed();
        markLongQueuedTasksAsFailed();
        rebroadcastUnassignedTasks();
        return true;
      }, 1L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException exception) {
      logger.error("Timed out processing delegate tasks");
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }

  private void markTimedOutTasksAsFailed() {
    long currentTime = clock.millis();
    Query<DelegateTask> longRunningTimedOutTasksQuery =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).filter("status", STARTED);
    longRunningTimedOutTasksQuery.and(new WhereCriteria("this.lastUpdatedAt + this.timeout < " + currentTime));

    List<Key<DelegateTask>> longRunningTimedOutTaskKeys = longRunningTimedOutTasksQuery.asKeyList();

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following timed out tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long and update their status to ERROR.
    Query<DelegateTask> longQueuedTasksQuery = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                   .filter("status", QUEUED)
                                                   .field("lastUpdatedAt")
                                                   .lessThan(clock.millis() - TimeUnit.HOURS.toMillis(1));

    List<Key<DelegateTask>> longQueuedTaskKeys = longQueuedTasksQuery.asKeyList();

    if (!longQueuedTaskKeys.isEmpty()) {
      List<String> keyList = longQueuedTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following long queued tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markTasksAsFailed(List<String> taskIds) {
    Query<DelegateTask> updateQuery =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).field(Mapper.ID_KEY).in(taskIds);
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", ERROR);
    wingsPersistence.update(updateQuery, updateOperations);

    List<DelegateTask> delegateTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                           .field(Mapper.ID_KEY)
                                           .in(taskIds)
                                           .project(Mapper.ID_KEY, true)
                                           .project("delegateId", true)
                                           .project("waitId", true)
                                           .asList();

    delegateTasks.forEach(delegateTask -> {
      if (isNotBlank(delegateTask.getWaitId())) {
        logger.info("Delegate task {} is terminated", delegateTask.getUuid());
        waitNotifyEngine.notify(delegateTask.getWaitId(),
            ErrorNotifyResponseData.builder()
                .errorMessage("Delegate timeout. Delegate ID: " + delegateTask.getDelegateId())
                .build());
      }
    });
  }

  void rebroadcastUnassignedTasks() {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    List<DelegateTask> unassignedTasks = null;
    unassignedTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                          .filter("status", QUEUED)
                          .filter("version", versionInfoManager.getVersionInfo().getVersion())
                          .field("delegateId")
                          .doesNotExist()
                          .asList();

    if (isNotEmpty(unassignedTasks)) {
      long now = clock.millis();
      unassignedTasks.forEach(delegateTask -> {
        if (delegateTask.getValidationStartedAt() == null
            || now - delegateTask.getValidationStartedAt() > VALIDATION_TIMEOUT) {
          logger.info("Re-broadcast queued task [{}]", delegateTask.getUuid());
          broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
        }
      });
    }
  }
}
