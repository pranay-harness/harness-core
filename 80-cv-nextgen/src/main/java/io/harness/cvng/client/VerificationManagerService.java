package io.harness.cvng.client;

public interface VerificationManagerService {
  String createServiceGuardDataCollectionTask(
      String accountId, String cvConfigId, String connectorId, String dataCollectionWorkerId);
  String createDeploymentVerificationDataCollectionTask(
      String accountId, String cvConfigId, String connectorId, String dataCollectionWorkerId);
  void deleteDataCollectionTask(String accountId, String taskId);
}
