package io.harness.perpetualtask;

import com.google.inject.Inject;

import io.grpc.ManagedChannel;
import io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceBlockingStub;
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
public class PerpetualTaskWorker implements Runnable {
  private static final int CORE_POOL_SIZE = 100;

  // TODO: add task assignment logic
  private final String delegateId = "";
  private Set<PerpetualTaskId> assignedTasks;
  // map of tasks currently in running state
  private Map<PerpetualTaskId, ScheduledFuture<?>> runningTaskMap;

  private final ScheduledExecutorService scheduledService;

  private final PerpetualTaskServiceBlockingStub serviceBlockingStub;
  private final PerpetualTaskFactory taskFactory;

  @Inject
  public PerpetualTaskWorker(ManagedChannel channel) {
    runningTaskMap = new ConcurrentHashMap<>();
    scheduledService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);

    taskFactory = new PerpetualTaskFactory();
    serviceBlockingStub = PerpetualTaskServiceGrpc.newBlockingStub(channel);
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
    assignedTasks = new HashSet<>(list.getTaskIdListList());
  }

  protected PerpetualTaskContext getTaskContext(PerpetualTaskId taskId) {
    return serviceBlockingStub.getTaskContext(taskId);
  }

  protected void startTask(PerpetualTaskId taskId) throws Exception {
    PerpetualTaskContext context = getTaskContext(taskId);
    PerpetualTaskParams params = context.getTaskParams();
    PerpetualTaskSchedule schedule = context.getTaskSchedule();
    long interval = schedule.getInterval();
    long timeout = schedule.getTimeout();
    // TODO: support timeout
    ScheduledFuture<?> taskHandle =
        scheduledService.scheduleAtFixedRate(taskFactory.newTask(taskId, params), 0, interval, TimeUnit.SECONDS);

    runningTaskMap.put(taskId, taskHandle);
  }

  public void startAssignedTasks() throws Exception {
    for (PerpetualTaskId taskId : assignedTasks) {
      // start the task if the task is not currently running
      if (!runningTaskMap.containsKey(taskId)) {
        logger.info("Starting the task with id: " + taskId.getId());
        startTask(taskId);
      }
    }
  }

  protected void stopTask(PerpetualTaskId taskId) {
    runningTaskMap.get(taskId).cancel(true);
    runningTaskMap.remove(taskId);
  }

  public void stopCancelledTasks() {
    for (PerpetualTaskId taskId : runningTaskMap.keySet()) {
      if (!assignedTasks.contains(taskId)) {
        logger.info("Stopping the task with id: " + taskId.getId());
        stopTask(taskId);
      }
    }
  }
}
