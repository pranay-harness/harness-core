package io.harness.cvng;

public interface DataCollectionTaskService {
  String create(String accountId, String cvConfigId);
  void delete(String accountId, String taskId);
}
