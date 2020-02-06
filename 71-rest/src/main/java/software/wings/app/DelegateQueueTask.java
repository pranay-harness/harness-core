package software.wings.app;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFilename;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.TaskLogContext;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HIterator;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.TaskType;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
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
@Slf4j
public class DelegateQueueTask implements Runnable {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private TimeLimiter timeLimiter;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (getMaintenanceFilename()) {
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
            .filter(DelegateTaskKeys.status, STARTED)
            .where("this." + DelegateTaskKeys.lastUpdatedAt + " + this." + DelegateTaskKeys.data_timeout + " < "
                + clock.millis())
            .asKeyList(new FindOptions().limit(100));

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      logger.info("Marking following timed out tasks as failed [{}]", keyList);
      markTasksAsFailed(keyList);
    }
  }

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long and update their status to ERROR.

    List<Key<DelegateTask>> longQueuedTaskKeys = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                     .filter(DelegateTaskKeys.status, QUEUED)
                                                     .where("this." + DelegateTaskKeys.createdAt + " + this."
                                                         + DelegateTaskKeys.data_timeout + " < " + clock.millis())
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
                                     .project(DelegateTaskKeys.delegateId, true)
                                     .project(DelegateTaskKeys.waitId, true)
                                     .project(DelegateTaskKeys.tags, true)
                                     .project(DelegateTaskKeys.accountId, true)
                                     .project(DelegateTaskKeys.data_taskType, true)
                                     .project(DelegateTaskKeys.data_parameters, true)
                                     .asList();
      delegateTasks.putAll(tasks.stream().collect(toMap(DelegateTask::getUuid, identity())));
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
                                  .project(DelegateTaskKeys.delegateId, true)
                                  .project(DelegateTaskKeys.waitId, true)
                                  .project(DelegateTaskKeys.tags, true)
                                  .project(DelegateTaskKeys.accountId, true)
                                  .project(DelegateTaskKeys.data_taskType, true)
                                  .project(DelegateTaskKeys.data_parameters, true)
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
                                .project(DelegateTaskKeys.waitId, true)
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
          waitNotifyEngine.doneWith(
              taskWaitIds.get(taskId), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
        }
      });
    }
  }

  private void rebroadcastUnassignedTasks() {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    long now = clock.millis();

    Query<DelegateTask> unassignedTasksQuery =
        wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter(DelegateTaskKeys.status, QUEUED)
            .filter(DelegateTaskKeys.version, versionInfoManager.getVersionInfo().getVersion())
            .field(DelegateTaskKeys.nextBroadcast)
            .lessThan(now)
            .field(DelegateTaskKeys.delegateId)
            .doesNotExist();

    try (HIterator<DelegateTask> iterator = new HIterator<>(unassignedTasksQuery.fetch())) {
      int count = 0;
      while (iterator.hasNext()) {
        DelegateTask delegateTask = iterator.next();
        Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                        .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                        .filter(DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount());

        UpdateOperations<DelegateTask> updateOperations =
            wingsPersistence.createUpdateOperations(DelegateTask.class)
                .set(DelegateTaskKeys.lastBroadcastAt, now)
                .set(DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount() + 1)
                .set(DelegateTaskKeys.nextBroadcast, broadcastHelper.findNextBroadcastTimeForTask(delegateTask))
                .unset(DelegateTaskKeys.preAssignedDelegateId);

        delegateTask =
            wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions().returnNew(true));
        // update failed, means this was broadcast by some other manager
        if (delegateTask == null) {
          continue;
        }

        try (AutoLogContext ignore1 = new TaskLogContext(delegateTask.getUuid(), delegateTask.getData().getTaskType(),
                 TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR);
             AutoLogContext ignore2 = new AccountLogContext(delegateTask.getAccountId(), OVERRIDE_ERROR)) {
          logger.info("Rebroadcast queued task. broadcast count: {}", delegateTask.getBroadcastCount());
          delegateTask.setPreAssignedDelegateId(null);
          broadcastHelper.rebroadcastDelegateTask(delegateTask);
          count++;
        }
      }

      logger.info("{} tasks were rebroadcast", count);
    }
  }
}
