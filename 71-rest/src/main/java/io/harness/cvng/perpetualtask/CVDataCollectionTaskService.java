package io.harness.cvng.perpetualtask;

public interface CVDataCollectionTaskService {
  String create(String accountId, String cvConfigId, String connectorId);
  void delete(String accountId, String taskId);
}
