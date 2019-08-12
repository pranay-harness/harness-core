package io.harness.perpetualtask;

import io.grpc.BindableService;

public interface PerpetualTaskService extends BindableService {
  String createTask(String clientName, String clientHandle, PerpetualTaskSchedule schedule);
  void deleteTask(String clientName, String clientHandle);
}
