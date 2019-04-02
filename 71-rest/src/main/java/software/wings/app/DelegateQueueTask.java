package software.wings.app;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.impl.DelegateServiceImpl.VALIDATION_TIMEOUT;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.query.WhereCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the DelegateQueueTask call", exception);
    }
  }

  private void markTimedOutTasksAsFailed() {
    List<Key<DelegateTask>> longRunningTimedOutTaskKeys =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter("status", STARTED)
            .where("this.lastUpdatedAt + this." + DelegateTask.DATA_TIMEOUT_KEY + " < " + clock.millis())
            .asKeyList(new FindOptions().limit(100));

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following timed out tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long and update their status to ERROR.

    List<Key<DelegateTask>> longQueuedTaskKeys =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter("status", QUEUED)
            .where("this.createdAt + this." + DelegateTask.DATA_TIMEOUT_KEY + " < " + clock.millis())
            .asKeyList(new FindOptions().limit(100));

    if (!longQueuedTaskKeys.isEmpty()) {
      List<String> keyList = longQueuedTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following long queued tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markTasksAsFailed(List<String> taskIds) {
    Map<String, DelegateTask> delegateTasks = new HashMap<>();
    Map<String, String> taskWaitIds = new HashMap<>();
    try {
      List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                     .field(ID_KEY)
                                     .in(taskIds)
                                     .project(ID_KEY, true)
                                     .project(DelegateTask.DELEGATE_ID_KEY, true)
                                     .project("waitId", true)
                                     .project("tags", true)
                                     .project("accountId", true)
                                     .project(DelegateTask.DATA_TASK_TYPE_KEY, true)
                                     .project(DelegateTask.DATA_PARAMETERS_KEY, true)
                                     .asList();
      delegateTasks.putAll(tasks.stream().collect(toMap(DelegateTask::getUuid, delegateTask -> delegateTask)));
      taskWaitIds.putAll(tasks.stream()
                             .filter(task -> isNotEmpty(task.getWaitId()))
                             .collect(toMap(DelegateTask::getUuid, DelegateTask::getWaitId)));
    } catch (Exception e1) {
      logger.error("Failed to deserialize {} tasks. Trying individually...", taskIds.size(), e1);
      for (String taskId : taskIds) {
        try {
          DelegateTask task = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                  .filter(ID_KEY, taskId)
                                  .project(ID_KEY, true)
                                  .project(DelegateTask.DELEGATE_ID_KEY, true)
                                  .project("waitId", true)
                                  .project("tags", true)
                                  .project("accountId", true)
                                  .project(DelegateTask.DATA_TASK_TYPE_KEY, true)
                                  .project(DelegateTask.DATA_PARAMETERS_KEY, true)
                                  .get();
          delegateTasks.put(taskId, task);
          if (isNotEmpty(task.getWaitId())) {
            taskWaitIds.put(taskId, task.getWaitId());
          }
        } catch (Exception e2) {
          logger.error("Could not deserialize task {}. Trying again with only waitId field.", taskId, e2);
          try {
            String waitId = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                .filter(ID_KEY, taskId)
                                .project("waitId", true)
                                .get()
                                .getWaitId();
            if (isNotEmpty(waitId)) {
              taskWaitIds.put(taskId, waitId);
            }
          } catch (Exception e3) {
            logger.error(
                "Could not deserialize task {} with waitId only, giving up. Task will be deleted but notify not called.",
                taskId, e3);
          }
        }
      }
    }

    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).field(Mapper.ID_KEY).in(taskIds));

    if (deleted) {
      taskIds.forEach(taskId -> {
        if (taskWaitIds.containsKey(taskId)) {
          String errorMessage = delegateTasks.containsKey(taskId)
              ? assignDelegateService.getActiveDelegateAssignmentErrorMessage(delegateTasks.get(taskId))
              : "Unable to determine proper error as delegate task could not be deserialized.";
          logger.info("Marking task as failed - {}: {}", taskId, errorMessage);
          waitNotifyEngine.notify(
              taskWaitIds.get(taskId), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
        }
      });
    }
  }

  private void rebroadcastUnassignedTasks() {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    Query<DelegateTask> unassignedTasksQuery = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                   .filter("status", QUEUED)
                                                   .filter("version", versionInfoManager.getVersionInfo().getVersion())
                                                   .field(DelegateTask.DELEGATE_ID_KEY)
                                                   .doesNotExist();

    long now = clock.millis();

    unassignedTasksQuery.and(
        unassignedTasksQuery.or(unassignedTasksQuery.criteria("validationStartedAt").doesNotExist(),
            unassignedTasksQuery.criteria("validationStartedAt").lessThan(now - VALIDATION_TIMEOUT)),
        unassignedTasksQuery.or(unassignedTasksQuery.criteria("lastBroadcastAt").doesNotExist(),
            new WhereCriteria(
                "this.lastBroadcastAt < " + now + " - Math.pow(2, this.broadcastCount) * " + REBROADCAST_FACTOR)));
    // TODO: there is a race between these two queries
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
