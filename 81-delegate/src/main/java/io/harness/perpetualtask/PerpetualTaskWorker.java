package io.harness.perpetualtask;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.grpc.ManagedChannel;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceBlockingStub;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerpetualTaskWorker implements Runnable {
  private static final int CORE_POOL_SIZE = 100;

  // TODO: add task assignment logic
  private final String delegateId = "";
  private Set<PerpetualTaskId> assignedTasks;
  // map of tasks currently in running state
  private Map<PerpetualTaskId, PerpetualTask> runningTaskMap;

  private final ScheduledExecutorService scheduledService;
  private final SimpleTimeLimiter simpleTimeLimiter;

  private final PerpetualTaskServiceBlockingStub serviceBlockingStub;

  private static Map<String, PerpetualTaskFactory> factoryMap;

  @Inject
  public PerpetualTaskWorker(ManagedChannel channel, Map<String, PerpetualTaskFactory> factoryMap) {
    runningTaskMap = new ConcurrentHashMap<>();
    scheduledService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
    simpleTimeLimiter = new SimpleTimeLimiter();

    serviceBlockingStub = PerpetualTaskServiceGrpc.newBlockingStub(channel);
    this.factoryMap = factoryMap;
  }

  @Override
  public void run() {
    try {
      updateAssignedTaskIds();
      stopCancelledTasks();
      startAssignedTasks();
    } catch (Exception e) {
      // TODO: handle exception
      logger.error(e.getStackTrace().toString());
    }
  }

  public void updateAssignedTaskIds() {
    logger.debug("Updating the list of assigned tasks..");
    PerpetualTaskIdList list = serviceBlockingStub.listTaskIds(DelegateId.newBuilder().setId(delegateId).build());
    assignedTasks = new HashSet<>(list.getTaskIdsList());
  }

  @VisibleForTesting
  protected Set<PerpetualTaskId> getAssignedTaskIds() {
    return assignedTasks;
  }

  protected PerpetualTaskContext getTaskContext(PerpetualTaskId taskId) {
    return serviceBlockingStub.getTaskContext(taskId);
  }

  protected void startTask(PerpetualTaskId taskId) throws Exception {
    PerpetualTaskContext context = getTaskContext(taskId);
    PerpetualTaskParams params = context.getTaskParams();
    PerpetualTaskSchedule schedule = context.getTaskSchedule();
    long interval = Durations.toSeconds(schedule.getInterval());
    long timeout = Durations.toMillis(schedule.getTimeout());

    PerpetualTaskFactory factory = factoryMap.get(getTaskType(params));
    PerpetualTask task = factory.newTask(taskId, params);
    runningTaskMap.put(taskId, task);

    Void v = simpleTimeLimiter.callWithTimeout(task, timeout, TimeUnit.MILLISECONDS, true);
    // TODO: support interval
    // ScheduledFuture<?> taskHandle =
    //    scheduledService.scheduleAtFixedRate(taskFactory.newTask(taskId, params), 0, interval, TimeUnit.SECONDS);

    // runningTaskMap.put(taskId, taskHandle);
  }

  public void startAssignedTasks() throws Exception {
    for (PerpetualTaskId taskId : this.getAssignedTaskIds()) {
      // start the task if the task is not currently running
      if (!runningTaskMap.containsKey(taskId)) {
        logger.info("Starting the task with id: {}", taskId.getId());
        startTask(taskId);
      }
    }
  }

  protected void stopTask(PerpetualTaskId taskId) {
    PerpetualTask task = runningTaskMap.get(taskId);
    task.stop();
    runningTaskMap.remove(taskId);
  }

  public void stopCancelledTasks() {
    for (PerpetualTaskId taskId : runningTaskMap.keySet()) {
      if (!this.getAssignedTaskIds().contains(taskId)) {
        logger.info("Stopping the task with id: {}", taskId.getId());
        stopTask(taskId);
      }
    }
  }

  private String getTaskType(PerpetualTaskParams params) {
    String fullyQualifiedClassName = AnyUtils.toFqcn(params.getCustomizedParams());
    return StringUtils.substringAfterLast(fullyQualifiedClassName, ".");
  }
}
