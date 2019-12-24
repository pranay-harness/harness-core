package io.harness.perpetualtask;

import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

public interface PerpetualTaskService {
  String createTask(PerpetualTaskType perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate);

  boolean resetTask(String accountId, String taskId);

  boolean deleteTask(String accountId, String taskId);

  List<String> listAssignedTaskIds(String delegateId);

  PerpetualTaskRecord getTaskRecord(String taskId);

  PerpetualTaskType getPerpetualTaskType(String taskId);

  PerpetualTaskContext getTaskContext(String taskId);

  boolean updateHeartbeat(String taskId, long heartbeatMillis);
}
