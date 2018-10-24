package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTask.Status.STARTED;
import static software.wings.service.impl.DelegateServiceImpl.VALIDATION_TIMEOUT;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.version.VersionInfoManager;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.query.WhereCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AssignDelegateService;
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

  private static final long REBROADCAST_FACTOR = TimeUnit.SECONDS.toMillis(2);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private TimeLimiter timeLimiter;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance()) {
      return;
    }

    try {
      timeLimiter.callWithTimeout(() -> {
        if (configurationController.isPrimary()) {
          markTimedOutTasksAsFailed();
          markLongQueuedTasksAsFailed();
        }
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
    List<Key<DelegateTask>> longRunningTimedOutTaskKeys =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter("status", STARTED)
            .where("this.lastUpdatedAt + this.timeout < " + clock.millis())
            .asKeyList();

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following timed out tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long and update their status to ERROR.

    List<Key<DelegateTask>> longQueuedTaskKeys = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                     .filter("status", QUEUED)
                                                     .where("this.createdAt + this.timeout < " + clock.millis())
                                                     .asKeyList();

    if (!longQueuedTaskKeys.isEmpty()) {
      List<String> keyList = longQueuedTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following long queued tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markTasksAsFailed(List<String> taskIds) {
    Query<DelegateTask> updateQuery =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).field(ID_KEY).in(taskIds);
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", ERROR);
    wingsPersistence.update(updateQuery, updateOperations);

    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).field(ID_KEY).in(taskIds).asList();

    delegateTasks.forEach(delegateTask -> {
      if (isNotBlank(delegateTask.getWaitId())) {
        String errorMessage = assignDelegateService.getActiveDelegateAssignmentErrorMessage(delegateTask);
        logger.info("Marking task as failed - {}: {}", delegateTask.getUuid(), errorMessage);
        waitNotifyEngine.notify(
            delegateTask.getWaitId(), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
      }
    });
  }

  private void rebroadcastUnassignedTasks() {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    Query<DelegateTask> unassignedTasksQuery = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                   .filter("status", QUEUED)
                                                   .filter("version", versionInfoManager.getVersionInfo().getVersion())
                                                   .field("delegateId")
                                                   .doesNotExist();

    long now = clock.millis();

    unassignedTasksQuery.and(
        unassignedTasksQuery.or(unassignedTasksQuery.criteria("validationStartedAt").doesNotExist(),
            unassignedTasksQuery.criteria("validationStartedAt").lessThan(now - VALIDATION_TIMEOUT)),
        unassignedTasksQuery.or(unassignedTasksQuery.criteria("lastBroadcastAt").doesNotExist(),
            new WhereCriteria(
                "this.lastBroadcastAt < " + now + " - Math.pow(2, this.broadcastCount) * " + REBROADCAST_FACTOR)));

    List<DelegateTask> unassignedTasks = unassignedTasksQuery.asList();
    if (isNotEmpty(unassignedTasks)) {
      UpdateResults results = wingsPersistence.update(unassignedTasksQuery,
          wingsPersistence.createUpdateOperations(DelegateTask.class)
              .set("lastBroadcastAt", now)
              .inc("broadcastCount")
              .unset("preAssignedDelegateId"));
      if (results.getUpdatedCount() > 0) {
        if (unassignedTasks.size() != results.getUpdatedCount()) {
          logger.info("Found {} tasks to rebroadcast. Updated {} tasks in DB.", unassignedTasks.size(),
              results.getUpdatedCount());
        } else {
          logger.info("Rebroadcasting {} tasks", unassignedTasks.size());
        }
        for (DelegateTask delegateTask : unassignedTasks) {
          logger.info("Rebroadcast queued task [{}], broadcast count: {}", delegateTask.getUuid(),
              delegateTask.getBroadcastCount());
          delegateTask.setPreAssignedDelegateId(null);
          broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
        }
      }
    } else {
      logger.info("No tasks found to rebroadcast");
    }
  }
}
