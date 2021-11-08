package software.wings.app;

import static io.harness.beans.DelegateTask.Status.PARKED;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.TaskFailureReason.EXPIRED;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.TaskLogContext;
import io.harness.exception.FailureType;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.TaskType;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 */
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateQueueTask implements Runnable {
  private static final SecureRandom random = new SecureRandom();

  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateService delegateService;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private ConfigurationController configurationController;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  @Override
  public void run() {
    if (getMaintenanceFlag()) {
      return;
    }

    try {
      if (configurationController.isPrimary()) {
        markTimedOutTasksAsFailed();
        markLongQueuedTasksAsFailed();
      }
      rebroadcastUnassignedTasks();
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    } catch (Exception exception) {
      log.error("Error seen in the DelegateQueueTask call", exception);
    }
  }

  private static final FindOptions expiryLimit = new FindOptions().limit(100);

  private void markTimedOutTasksAsFailed() {
    List<Key<DelegateTask>> longRunningTimedOutTaskKeys = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                                              .filter(DelegateTaskKeys.status, STARTED)
                                                              .field(DelegateTaskKeys.expiry)
                                                              .lessThan(currentTimeMillis())
                                                              .asKeyList(expiryLimit);

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      log.info("Marking following timed out tasks as failed [{}]", keyList);
      endTasks(keyList);
    }
  }

  private AtomicInteger clustering = new AtomicInteger(1);

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long
    Query<DelegateTask> query = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                    .field(DelegateTaskKeys.status)
                                    .in(asList(QUEUED, PARKED))
                                    .field(DelegateTaskKeys.expiry)
                                    .lessThan(currentTimeMillis());

    // We usually pick from the top, but if we have full bucket we maybe slowing down
    // lets randomize a bit to increase the distribution
    int clusteringValue = clustering.get();
    if (clusteringValue > 1) {
      query.field(DelegateTaskKeys.createdAt).mod(clusteringValue, random.nextInt(clusteringValue));
    }

    List<Key<DelegateTask>> longQueuedTaskKeys = query.asKeyList(expiryLimit);
    clustering.set(longQueuedTaskKeys.size() == expiryLimit.getLimit() ? Math.min(16, clusteringValue * 2)
                                                                       : Math.max(1, clusteringValue / 2));

    if (!longQueuedTaskKeys.isEmpty()) {
      List<String> keyList = longQueuedTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      log.info("Marking following long queued tasks as failed [{}]", keyList);
      endTasks(keyList);
    }
  }

  @VisibleForTesting
  public void endTasks(List<String> taskIds) {
    Map<String, DelegateTask> delegateTasks = new HashMap<>();
    Map<String, String> taskWaitIds = new HashMap<>();
    List<DelegateTask> tasksToExpire = new ArrayList<>();
    List<String> taskIdsToExpire = new ArrayList<>();
    try {
      List<DelegateTask> tasks = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                     .field(DelegateTaskKeys.uuid)
                                     .in(taskIds)
                                     .asList();

      for (DelegateTask task : tasks) {
        if (shouldExpireTask(task)) {
          tasksToExpire.add(task);
          taskIdsToExpire.add(task.getUuid());
        }
      }

      delegateTasks.putAll(tasksToExpire.stream().collect(toMap(DelegateTask::getUuid, identity())));
      taskWaitIds.putAll(tasksToExpire.stream()
                             .filter(task -> isNotEmpty(task.getWaitId()))
                             .collect(toMap(DelegateTask::getUuid, DelegateTask::getWaitId)));
    } catch (Exception e1) {
      log.error("Failed to deserialize {} tasks. Trying individually...", taskIds.size(), e1);
      for (String taskId : taskIds) {
        try {
          DelegateTask task =
              persistence.createQuery(DelegateTask.class, excludeAuthority).filter(DelegateTaskKeys.uuid, taskId).get();
          if (shouldExpireTask(task)) {
            taskIdsToExpire.add(taskId);
            delegateTasks.put(taskId, task);
            if (isNotEmpty(task.getWaitId())) {
              taskWaitIds.put(taskId, task.getWaitId());
            }
          }
        } catch (Exception e2) {
          log.error("Could not deserialize task {}. Trying again with only waitId field.", taskId, e2);
          taskIdsToExpire.add(taskId);
          try {
            String waitId = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                .filter(DelegateTaskKeys.uuid, taskId)
                                .project(DelegateTaskKeys.waitId, true)
                                .get()
                                .getWaitId();
            if (isNotEmpty(waitId)) {
              taskWaitIds.put(taskId, waitId);
            }
          } catch (Exception e3) {
            log.error(
                "Could not deserialize task {} with waitId only, giving up. Task will be deleted but notify not called.",
                taskId, e3);
          }
        }
      }
    }

    boolean deleted = persistence.deleteOnServer(
        persistence.createQuery(DelegateTask.class, excludeAuthority).field(DelegateTaskKeys.uuid).in(taskIdsToExpire));

    if (deleted) {
      taskIdsToExpire.forEach(taskId -> {
        if (taskWaitIds.containsKey(taskId)) {
          String errorMessage = delegateTasks.containsKey(taskId)
              ? assignDelegateService.getActiveDelegateAssignmentErrorMessage(EXPIRED, delegateTasks.get(taskId))
              : "Unable to determine proper error as delegate task could not be deserialized.";
          log.info("Marking task as failed - {}: {}", taskId, errorMessage);

          if (delegateTasks.get(taskId) != null) {
            if (delegateTasks.get(taskId).getData().getTaskType().equals(TaskType.SCRIPT.name())) {
              delegateTaskService.handleResponse(delegateTasks.get(taskId), null,
                  DelegateTaskResponse.builder()
                      .accountId(delegateTasks.get(taskId).getAccountId())
                      .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
                      .expired(true)
                      .response(ErrorNotifyResponseData.builder()
                                    .errorMessage(errorMessage)
                                    .failureTypes(EnumSet.of(FailureType.TIMEOUT_ERROR))
                                    .build())
                      .build());
            } else {
              delegateTaskService.handleResponse(delegateTasks.get(taskId), null,
                  DelegateTaskResponse.builder()
                      .accountId(delegateTasks.get(taskId).getAccountId())
                      .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
                      .response(ErrorNotifyResponseData.builder().errorMessage(errorMessage).build())
                      .build());
            }
          }
        }
      });
    }
  }

  @VisibleForTesting
  protected void rebroadcastUnassignedTasks() {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    long now = clock.millis();

    Query<DelegateTask> unassignedTasksQuery =
        persistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter(DelegateTaskKeys.status, QUEUED)
            .filter(DelegateTaskKeys.version, versionInfoManager.getVersionInfo().getVersion())
            .field(DelegateTaskKeys.nextBroadcast)
            .lessThan(now)
            .field(DelegateTaskKeys.expiry)
            .greaterThan(now)
            .field(DelegateTaskKeys.delegateId)
            .doesNotExist();

    try (HIterator<DelegateTask> iterator = new HIterator<>(unassignedTasksQuery.fetch())) {
      int count = 0;
      while (iterator.hasNext()) {
        DelegateTask delegateTask = iterator.next();
        Query<DelegateTask> query = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                        .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                        .filter(DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount());

        UpdateOperations<DelegateTask> updateOperations =
            persistence.createUpdateOperations(DelegateTask.class)
                .set(DelegateTaskKeys.lastBroadcastAt, now)
                .set(DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount() + 1)
                .set(DelegateTaskKeys.nextBroadcast, broadcastHelper.findNextBroadcastTimeForTask(delegateTask));

        // Old way with rebroadcasting
        if (featureFlagService.isNotEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId())
            && delegateTask.getPreAssignedDelegateId() != null && delegateTask.getMustExecuteOnDelegateId() == null
            && delegateTask.getBroadcastCount() > 0) {
          updateOperations.unset(DelegateTaskKeys.preAssignedDelegateId);
        }

        // New way with per agent capabilities. Batch capabilities check task is using mustExecuteOnDelegateIs and we
        // must respect it
        if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegateTask.getAccountId())
            && isBlank(delegateTask.getMustExecuteOnDelegateId())) {
          if (delegateTask.getPreAssignedDelegateId() != null) {
            updateOperations.addToSet(DelegateTaskKeys.alreadyTriedDelegates, delegateTask.getPreAssignedDelegateId());
          }

          setUnset(updateOperations, DelegateTaskKeys.preAssignedDelegateId,
              delegateTaskServiceClassic.obtainCapableDelegateId(
                  delegateTask, delegateTask.getAlreadyTriedDelegates()));
        }

        delegateTask = persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);
        // update failed, means this was broadcast by some other manager
        if (delegateTask == null) {
          continue;
        }

        try (AutoLogContext ignore1 = new TaskLogContext(delegateTask.getUuid(), delegateTask.getData().getTaskType(),
                 TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR);
             AutoLogContext ignore2 = new AccountLogContext(delegateTask.getAccountId(), OVERRIDE_ERROR)) {
          log.info("Rebroadcast queued task. broadcast count: {}", delegateTask.getBroadcastCount());
          broadcastHelper.rebroadcastDelegateTask(delegateTask);
          count++;
        }
      }

      log.info("{} tasks were rebroadcast", count);
    }
  }

  private boolean handleTaskWithForceExecution(DelegateTask task) {
    BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);
    List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(task.getAccountId(), batch);

    for (String delegateId : activeDelegates) {
      boolean canAssign = assignDelegateService.canAssign(batch, delegateId, task);
      if (canAssign) {
        task.setPreAssignedDelegateId(delegateId);
        break;
      }
    }

    // If unable to assign any delegate for the task, then we expire it
    if (task.getPreAssignedDelegateId() == null) {
      return false;
    }

    delegateSelectionLogsService.save(batch);

    Query<DelegateTask> filterQuery = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                          .filter(DelegateTaskKeys.accountId, task.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, task.getUuid())
                                          .filter(DelegateTaskKeys.status, QUEUED);

    UpdateOperations<DelegateTask> updateOperations =
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.nextBroadcast, 0)
            .set(DelegateTaskKeys.expiry, currentTimeMillis() + task.fetchExtraTimeoutForForceExecution())
            .set(DelegateTaskKeys.preAssignedDelegateId, task.getPreAssignedDelegateId())
            .set(DelegateTaskKeys.mustExecuteOnDelegateId, task.getPreAssignedDelegateId())
            .set(DelegateTaskKeys.forceExecute, false);

    persistence.findAndModify(filterQuery, updateOperations, HPersistence.returnNewOptions);

    log.info("Setting task for force execution : {}", task);

    return true;
  }

  private boolean shouldExpireTask(DelegateTask task) {
    return !task.isForceExecute() || !handleTaskWithForceExecution(task);
  }
}
