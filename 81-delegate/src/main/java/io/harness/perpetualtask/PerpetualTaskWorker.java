package io.harness.perpetualtask;

import static io.harness.delegate.service.DelegateAgentServiceImpl.getDelegateId;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.System.currentTimeMillis;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;

import io.grpc.StatusRuntimeException;
import io.harness.flow.BackoffScheduler;
import io.harness.logging.AutoLogContext;
import io.harness.logging.LoggingListener;
import io.harness.mongo.DelayLogContext;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import io.harness.threading.Schedulable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class PerpetualTaskWorker {
  @Getter private final Map<PerpetualTaskId, PerpetualTaskAssignRecord> runningTaskMap = new ConcurrentHashMap<>();

  private TimeLimiter perpetualTaskTimeLimiter;
  private ScheduledExecutorService perpetualTaskTimeoutExecutor;

  private final AtomicBoolean firstFillUp = new AtomicBoolean(true);
  private final BackoffScheduler backoffScheduler;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;
  private Map<String, PerpetualTaskExecutor> factoryMap;

  private AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<PerpetualTaskWorkerService> svcHolder = new AtomicReference<>();

  private class PerpetualTaskWorkerService extends AbstractScheduledService {
    PerpetualTaskWorkerService() {
      addListener(new LoggingListener(this), MoreExecutors.directExecutor());
    }

    @Override
    protected void runOneIteration() {
      handleTasks();
    }

    @Override
    protected Scheduler scheduler() {
      return backoffScheduler;
    }
  }

  @Inject
  public PerpetualTaskWorker(PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient,
      Map<String, PerpetualTaskExecutor> factoryMap,
      @Named("perpetualTaskExecutor") ExecutorService perpetualTaskExecutor,
      @Named("perpetualTaskTimeoutExecutor") ScheduledExecutorService perpetualTaskTimeoutExecutor) {
    this.perpetualTaskServiceGrpcClient = perpetualTaskServiceGrpcClient;
    this.factoryMap = factoryMap;
    this.perpetualTaskTimeLimiter = new SimpleTimeLimiter(perpetualTaskExecutor);
    this.perpetualTaskTimeoutExecutor = perpetualTaskTimeoutExecutor;

    backoffScheduler = new BackoffScheduler(getClass().getSimpleName(), Duration.ofMinutes(4), Duration.ofMinutes(14));
  }

  private void handleTasks() {
    try {
      List<PerpetualTaskAssignDetails> assignedTasks = fetchAssignedTask();
      Set<PerpetualTaskId> stopTasks = new HashSet<>();
      List<PerpetualTaskAssignDetails> startTasks = new ArrayList<>();
      List<PerpetualTaskAssignDetails> updatedTasks = new ArrayList<>();
      synchronized (runningTaskMap) {
        splitTasks(runningTaskMap, assignedTasks, stopTasks, startTasks, updatedTasks);
      }

      for (PerpetualTaskId taskId : stopTasks) {
        logger.info("Stopping the task with id: {}", taskId.getId());
        stopTask(taskId);
      }

      for (PerpetualTaskAssignDetails task : startTasks) {
        if (!firstFillUp.get()) {
          logPullDelay(task, "first poll from this delegate for task");
        }
        startTask(task);
      }
      firstFillUp.set(false);

      for (PerpetualTaskAssignDetails task : updatedTasks) {
        logPullDelay(task, "update for task");
        stopTask(task.getTaskId());
        startTask(task);
      }

      backoffScheduler.recordSuccess();
    } catch (StatusRuntimeException ex) {
      logger.error("Grpc status exception in perpetual task worker. Backing off...", ex);
      backoffScheduler.recordFailure();
    } catch (Exception ex) {
      logger.error("Exception in perpetual task worker ", ex);
    }
  }

  private void logPullDelay(PerpetualTaskAssignDetails task, String message) {
    long lastContextUpdated = Timestamps.toMillis(task.getLastContextUpdated());
    long startTime = currentTimeMillis();
    long delay = startTime - lastContextUpdated;

    try (DelayLogContext ignore = new DelayLogContext(delay, OVERRIDE_ERROR)) {
      logger.info(message);
    }
  }

  protected void splitTasks(Map<PerpetualTaskId, PerpetualTaskAssignRecord> runningTaskMap,
      List<PerpetualTaskAssignDetails> assignedTasks, Set<PerpetualTaskId> stopTasks,
      List<PerpetualTaskAssignDetails> startTasks, List<PerpetualTaskAssignDetails> updatedTasks) {
    stopTasks.addAll(runningTaskMap.keySet());

    for (PerpetualTaskAssignDetails assignDetails : assignedTasks) {
      if (!stopTasks.remove(assignDetails.getTaskId())) {
        startTasks.add(assignDetails);
      } else {
        PerpetualTaskAssignRecord runningTask = runningTaskMap.get(assignDetails.getTaskId());
        long runningTaskLastContextUpdated =
            Timestamps.toMillis(runningTask.getPerpetualTaskAssignDetails().getLastContextUpdated());
        long assignDetailsLastContextUpdated = Timestamps.toMillis(assignDetails.getLastContextUpdated());
        if (runningTaskLastContextUpdated < assignDetailsLastContextUpdated) {
          updatedTasks.add(assignDetails);
        }
      }
    }
  }

  List<PerpetualTaskAssignDetails> fetchAssignedTask() {
    String delegateId = getDelegateId().orElse("UNREGISTERED");
    List<PerpetualTaskAssignDetails> assignedTasks = perpetualTaskServiceGrpcClient.perpetualTaskList(delegateId);
    if (logger.isDebugEnabled()) {
      logger.debug("Refreshed list of assigned perpetual tasks {}", assignedTasks);
    }
    return assignedTasks;
  }

  @VisibleForTesting
  void startTask(PerpetualTaskAssignDetails task) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(task.getTaskId().getId(), OVERRIDE_ERROR)) {
      PerpetualTaskExecutionContext context = perpetualTaskServiceGrpcClient.perpetualTaskContext(task.getTaskId());
      PerpetualTaskSchedule schedule = context.getTaskSchedule();
      long intervalSeconds = Durations.toSeconds(schedule.getInterval());

      PerpetualTaskLifecycleManager perpetualTaskLifecycleManager = new PerpetualTaskLifecycleManager(
          task.getTaskId(), context, factoryMap, perpetualTaskServiceGrpcClient, perpetualTaskTimeLimiter);

      synchronized (runningTaskMap) {
        runningTaskMap.computeIfAbsent(task.getTaskId(), k -> {
          logger.info("Starting perpetual task with id: {}.", task.getTaskId().getId());
          ScheduledFuture<?> taskHandle = perpetualTaskTimeoutExecutor.scheduleWithFixedDelay(
              new Schedulable("Throwable while executing perpetual task", perpetualTaskLifecycleManager::startTask), 0,
              intervalSeconds, TimeUnit.SECONDS);

          PerpetualTaskHandle perpetualTaskHandle = new PerpetualTaskHandle(taskHandle, perpetualTaskLifecycleManager);

          return PerpetualTaskAssignRecord.builder()
              .perpetualTaskHandle(perpetualTaskHandle)
              .perpetualTaskAssignDetails(task)
              .build();
        });
      }
    } catch (Exception ex) {
      logger.error("Exception in starting perpetual task ", ex);
    }
  }

  @VisibleForTesting
  void stopTask(PerpetualTaskId taskId) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      PerpetualTaskAssignRecord perpetualTaskAssignRecord;
      synchronized (runningTaskMap) {
        perpetualTaskAssignRecord = runningTaskMap.get(taskId);
        if (perpetualTaskAssignRecord == null) {
          return;
        }
        runningTaskMap.remove(taskId);
      }
      logger.info("Stopping perpetual task with id: {}.", taskId.getId());
      perpetualTaskAssignRecord.getPerpetualTaskHandle().getTaskLifecycleManager().stopTask();
      perpetualTaskAssignRecord.getPerpetualTaskHandle().getTaskHandle().cancel(true);
    }
  }

  public void updateTasks() {
    handleTasks();
  }

  public void start() {
    if (running.compareAndSet(false, true)) {
      PerpetualTaskWorkerService perpetualTaskWorkerService = new PerpetualTaskWorkerService();
      perpetualTaskWorkerService.startAsync();
      this.svcHolder.set(perpetualTaskWorkerService);
    }
  }
  public void stop() {
    if (running.compareAndSet(true, false)) {
      PerpetualTaskWorkerService perpetualTaskWorkerService = this.svcHolder.get();
      perpetualTaskWorkerService.stopAsync().awaitTerminated();
      while (true) {
        synchronized (runningTaskMap) {
          Iterator<PerpetualTaskId> iterator = runningTaskMap.keySet().iterator();
          if (!iterator.hasNext()) {
            break;
          }
          stopTask(iterator.next());
        }
      }
    }
  }
}
