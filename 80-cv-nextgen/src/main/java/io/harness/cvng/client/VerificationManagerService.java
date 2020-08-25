package io.harness.cvng.client;

public interface VerificationManagerService {
  String createServiceGuardDataCollectionTask(String accountId, String cvConfigId, String connectorId,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId);
  String createDeploymentVerificationDataCollectionTask(String accountId, String connectorId, String orgIdentifier,
      String projectIdentifier, String dataCollectionWorkerId);
  void deleteDataCollectionTask(String accountId, String taskId);
}
