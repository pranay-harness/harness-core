package io.harness.cvng;

public interface CVDataCollectionTaskService {
  String create(String accountId, String cvConfigId);
  void delete(String accountId, String taskId);
}
