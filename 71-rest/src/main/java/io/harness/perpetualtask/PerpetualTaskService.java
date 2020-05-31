package io.harness.perpetualtask;

import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

public interface PerpetualTaskService {
  String createTask(PerpetualTaskType perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate);

  boolean resetTask(String accountId, String taskId);

  boolean deleteTask(String accountId, String taskId);

  List<PerpetualTaskAssignDetails> listAssignedTasks(String delegateId);

  PerpetualTaskRecord getTaskRecord(String taskId);

  PerpetualTaskType getPerpetualTaskType(String taskId);

  PerpetualTaskExecutionContext perpetualTaskContext(String taskId);

  boolean triggerCallback(String taskId, long heartbeatMillis, PerpetualTaskResponse perpetualTaskResponse);

  void appointDelegate(String taskId, String delegateId, long lastContextUpdated);

  void setTaskState(String taskId, String state);
}
