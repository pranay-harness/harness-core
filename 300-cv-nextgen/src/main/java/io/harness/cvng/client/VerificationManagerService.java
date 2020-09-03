package io.harness.cvng.client;

public interface VerificationManagerService {
  String createServiceGuardDataCollectionTask(String accountId, String cvConfigId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId);
  String createDeploymentVerificationDataCollectionTask(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId);
  void deleteDataCollectionTask(String accountId, String taskId);
}
