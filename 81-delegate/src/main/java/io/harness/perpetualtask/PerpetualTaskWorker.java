package io.harness.perpetualtask;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class PerpetualTaskWorker {
  // TODO(Tang): add task assignment logic
  private final String delegateId = "";
  private final TimeLimiter timeLimiter;
  private Set<PerpetualTaskId> assignedTasks;
  private Map<String, PerpetualTaskExecutor> factoryMap;
  private final ScheduledExecutorService scheduledService;
  private Map<PerpetualTaskId, PerpetualTaskHandle> runningTaskMap;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;

  @Inject
  public PerpetualTaskWorker(PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient,
      Map<String, PerpetualTaskExecutor> factoryMap, TimeLimiter timeLimiter) {
    this.factoryMap = factoryMap;
    this.timeLimiter = timeLimiter;
    this.perpetualTaskServiceGrpcClient = perpetualTaskServiceGrpcClient;
    runningTaskMap = new ConcurrentHashMap<>();
    scheduledService = Executors.newSingleThreadScheduledExecutor();
  }

  public void handleTasks() {
    try {
      updateAssignedTaskIds();
      stopCancelledTasks();
      startAssignedTasks();
    } catch (Exception ex) {
      // TODO(Tang): handle exception
      logger.error("Exception in perpetual task worker ", ex);
    }
  }

  void updateAssignedTaskIds() {
    logger.debug("Updating the list of assigned tasks.. {} ", delegateId);
    assignedTasks = new HashSet<>(perpetualTaskServiceGrpcClient.listTaskIds(delegateId));
  }

  @VisibleForTesting
  Set<PerpetualTaskId> getAssignedTaskIds() {
    return assignedTasks;
  }

  void startTask(PerpetualTaskId taskId) {
    try {
      PerpetualTaskContext context = perpetualTaskServiceGrpcClient.getTaskContext(taskId);
      PerpetualTaskSchedule schedule = context.getTaskSchedule();
      long intervalSeconds = Durations.toSeconds(schedule.getInterval());

      PerpetualTaskLifecycleManager perpetualTaskLifecycleManager =
          new PerpetualTaskLifecycleManager(taskId, context, factoryMap, perpetualTaskServiceGrpcClient, timeLimiter);
      ScheduledFuture<?> taskHandle = scheduledService.scheduleWithFixedDelay(
          perpetualTaskLifecycleManager::startTask, 0, intervalSeconds, TimeUnit.SECONDS);
      PerpetualTaskHandle perpetualTaskHandle = new PerpetualTaskHandle(taskHandle, perpetualTaskLifecycleManager);
      runningTaskMap.put(taskId, perpetualTaskHandle);
    } catch (Exception ex) {
      logger.error("Exception in starting perpetual task ", ex);
    }
  }

  private void startAssignedTasks() {
    for (PerpetualTaskId taskId : this.getAssignedTaskIds()) {
      // start the task if the task is not currently running
      if (!runningTaskMap.containsKey(taskId)) {
        logger.info("Starting the task with id: {}", taskId.getId());
        startTask(taskId);
      }
    }
  }

  private void stopTask(PerpetualTaskId taskId) {
    PerpetualTaskHandle perpetualTaskHandle = runningTaskMap.get(taskId);
    perpetualTaskHandle.getTaskLifecycleManager().stopTask();
    perpetualTaskHandle.getTaskHandle().cancel(true);
    runningTaskMap.remove(taskId);
  }

  private void stopCancelledTasks() {
    for (PerpetualTaskId taskId : runningTaskMap.keySet()) {
      if (!this.getAssignedTaskIds().contains(taskId)) {
        logger.info("Stopping the task with id: {}", taskId.getId());
        stopTask(taskId);
      }
    }
  }
}
