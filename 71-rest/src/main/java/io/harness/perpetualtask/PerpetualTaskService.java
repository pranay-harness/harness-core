package io.harness.perpetualtask;

import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

public interface PerpetualTaskService {
  String createTask(String perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate, String taskDescription);

  boolean resetTask(String accountId, String taskId, PerpetualTaskExecutionBundle taskExecutionBundle);

  boolean deleteTask(String accountId, String taskId);

  List<PerpetualTaskAssignDetails> listAssignedTasks(String delegateId);

  PerpetualTaskRecord getTaskRecord(String taskId);

  String getPerpetualTaskType(String taskId);

  PerpetualTaskExecutionContext perpetualTaskContext(String taskId);

  boolean triggerCallback(String taskId, long heartbeatMillis, PerpetualTaskResponse perpetualTaskResponse);

  void appointDelegate(String accountId, String taskId, String delegateId, long lastContextUpdated);

  void setTaskState(String taskId, String state);
}
