package io.harness.perpetualtask;

import io.harness.exception.WingsException;

import java.util.List;

public interface PerpetualTaskService {
  void createTask(String clientName, String clientHandle, PerpetualTaskSchedule schedule) throws WingsException;
  void deleteTask(String clientName, String clientHandle) throws WingsException;
  List<PerpetualTaskId> listTaskIds(String delegateId);
  PerpetualTaskContext getTaskContext(String taskId);
}
