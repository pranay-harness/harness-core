package io.harness.perpetualtask;

import java.util.List;

public interface PerpetualTaskService {
  String createTask(PerpetualTaskType perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate);

  boolean deleteTask(String accountId, String taskId);

  List<String> listAssignedTaskIds(String delegateId);

  PerpetualTaskContext getTaskContext(String taskId);

  boolean updateHeartbeat(String taskId, long heartbeatMillis);
}
