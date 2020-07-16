package io.harness.cvng.client;

public interface VerificationManagerService {
  String createDataCollectionTask(String accountId, String cvConfigId, String connectorId);
  void deleteDataCollectionTask(String accountId, String taskId);
}
