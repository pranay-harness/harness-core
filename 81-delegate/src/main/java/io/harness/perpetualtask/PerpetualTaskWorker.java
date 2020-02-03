package io.harness.perpetualtask;

import static io.harness.delegate.service.DelegateServiceImpl.getDelegateId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PerpetualTaskWorker extends AbstractScheduledService {
  private static final int PERPETUAL_TASK_POLL_INTERVAL_MINUTES = 1;

  @Getter private Set<PerpetualTaskId> assignedTasks;
  @Getter private final Map<PerpetualTaskId, PerpetualTaskHandle> runningTaskMap = new ConcurrentHashMap<>();
  private ScheduledExecutorService scheduledService;

  private final TimeLimiter timeLimiter;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;
  private Map<String, PerpetualTaskExecutor> factoryMap;

  @Inject
  public PerpetualTaskWorker(PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient,
      Map<String, PerpetualTaskExecutor> factoryMap, TimeLimiter timeLimiter) {
    this.factoryMap = factoryMap;
    this.timeLimiter = timeLimiter;
    this.perpetualTaskServiceGrpcClient = perpetualTaskServiceGrpcClient;
    scheduledService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("perpetual-task-worker").build());
  }

  private void handleTasks() {
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
    String delegateId = getDelegateId().orElse("UNREGISTERED");
    assignedTasks = new HashSet<>(perpetualTaskServiceGrpcClient.listTaskIds(delegateId));
    logger.debug("Refreshed list of assigned perpetual tasks {}", assignedTasks);
  }

  @VisibleForTesting
  void startTask(PerpetualTaskId taskId) {
    try {
      logger.info("Starting perpetual task with id: {}.", taskId);
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

  @VisibleForTesting
  void startAssignedTasks() {
    if (assignedTasks == null) {
      return;
    }
    for (PerpetualTaskId taskId : assignedTasks) {
      // start the task if the task is not currently running
      if (!runningTaskMap.containsKey(taskId)) {
        startTask(taskId);
      }
    }
  }

  @VisibleForTesting
  void stopTask(PerpetualTaskId taskId) {
    PerpetualTaskHandle perpetualTaskHandle = runningTaskMap.get(taskId);
    if (perpetualTaskHandle == null) {
      logger.error(
          "The request to delete a task with id={} cannot be fulfilled since such task does not exist.", taskId);
      return;
    }
    perpetualTaskHandle.getTaskLifecycleManager().stopTask();
    perpetualTaskHandle.getTaskHandle().cancel(true);
    runningTaskMap.remove(taskId);
  }

  private void stopCancelledTasks() {
    for (PerpetualTaskId taskId : runningTaskMap.keySet()) {
      if (!assignedTasks.contains(taskId)) {
        logger.info("Stopping the task with id: {}", taskId.getId());
        stopTask(taskId);
      }
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    handleTasks();
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, PERPETUAL_TASK_POLL_INTERVAL_MINUTES, TimeUnit.MINUTES);
  }
}
