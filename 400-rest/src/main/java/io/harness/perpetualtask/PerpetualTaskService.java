package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public interface PerpetualTaskService {
  String createTask(String perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate, String taskDescription);

  boolean resetTask(String accountId, String taskId, PerpetualTaskExecutionBundle taskExecutionBundle);

  boolean deleteTask(String accountId, String taskId);

  boolean pauseTask(String accountId, String taskId);

  boolean resumeTask(String accountId, String taskId);

  boolean deleteAllTasksForAccount(String accountId);

  List<PerpetualTaskAssignDetails> listAssignedTasks(String delegateId);

  List<PerpetualTaskRecord> listAllTasksForAccount(String accountId);

  PerpetualTaskRecord getTaskRecord(String taskId);

  String getPerpetualTaskType(String taskId);

  PerpetualTaskExecutionContext perpetualTaskContext(String taskId);

  boolean triggerCallback(String taskId, long heartbeatMillis, PerpetualTaskResponse perpetualTaskResponse);

  void appointDelegate(String accountId, String taskId, String delegateId, long lastContextUpdated);

  void updateTaskUnassignedReason(String taskId, PerpetualTaskUnassignedReason reason);
}
