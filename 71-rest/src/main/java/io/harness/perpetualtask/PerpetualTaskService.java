package io.harness.perpetualtask;

import java.util.List;

public interface PerpetualTaskService {
  String createTask(PerpetualTaskType perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate);

  boolean deleteTask(String accountId, String taskId);

  List<String> listTaskIds(String delegateId);

  PerpetualTaskRecord getTask(String taskId);
}
